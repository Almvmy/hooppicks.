package com.hooppicks.backendapplication.chat;

import com.hooppicks.backendapplication.dto.PlayerBoxScoreDto;
import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.live.LiveMatchService.LiveStatusDto;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.NightMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Messages automatiques du fil de la soirée : fins de quart-temps, résultats,
 * grosses perfs, gros gains. Pour que le fil vive même quand personne n'écrit :
 * avec une vingtaine de joueurs, une salle vide ferait plus « mort » que pas de
 * salle du tout. Chacun ne part qu'une fois (clé `auto:…` en base).
 */
@Service
public class NightThreadEvents {

    // Assez rare pour rester un événement : un bénéfice de 300 pts, c'est un beau coup.
    static final int BIG_WIN = 300;
    private static final List<Integer> SCORING_MARKS = List.of(50, 40, 30);

    private final NightMessageRepository messageRepository;
    private final MatchRepository matchRepository;
    private final BetRepository betRepository;

    public NightThreadEvents(NightMessageRepository messageRepository, MatchRepository matchRepository, BetRepository betRepository) {
        this.messageRepository = messageRepository;
        this.matchRepository = matchRepository;
        this.betRepository = betRepository;
    }

    /** `board` et `boxes` sont lus chez ESPN avant d'arriver ici : jamais d'appel réseau dans la transaction. */
    @Transactional
    public int record(List<LiveStatusDto> board, Map<String, List<PlayerBoxScoreDto>> boxes, Instant now) {
        int posted = 0;
        for (LiveStatusDto live : board) {
            Match match = matchRepository.findById(live.matchId()).orElse(null);
            // Le direct garde aussi les matchs de la veille : ils n'ont rien à faire dans le fil de ce soir.
            if (match == null || match.getDate() == null
                    || !NightThreadService.nightOf(match.getDate()).equals(NightThreadService.nightOf(now))) continue;
            posted += recordMatch(match, live, boxes.getOrDefault(live.matchId(), List.of()), now);
        }
        for (Bet bet : betRepository.findWonSince(now.minus(Duration.ofMinutes(15)))) {
            int profit = bet.getPotentialPayout() - bet.getStake();
            if (profit < BIG_WIN) continue;
            posted += post("win:" + bet.getId(), bet.getUser().getUsername() + " encaisse +" + profit + " pts", null, now);
        }
        return posted;
    }

    private int recordMatch(Match match, LiveStatusDto live, List<PlayerBoxScoreDto> players, Instant now) {
        String id = match.getId();
        String score = score(match, live);
        int posted = 0;
        if ("post".equals(live.state())) {
            return post("final:" + id, "Terminé : " + score, id, now);
        }
        if ("in".equals(live.state()) && periodEnded(live)) {
            String what = live.period() == 2 ? "Mi-temps" : "Fin " + periodName(live.period());
            posted += post("period:" + id + ":" + live.period(), what + " : " + score, id, now);
        }
        for (PlayerBoxScoreDto p : players) {
            // Le palier le plus haut atteint seulement : un seul message si la feuille arrive en retard.
            for (int mark : SCORING_MARKS) {
                if (p.points() < mark) continue;
                posted += post("points:" + id + ":" + p.playerName() + ":" + mark, p.playerName() + " passe les " + mark + " pts", id, now);
                break;
            }
        }
        return posted;
    }

    private int post(String key, String text, String matchId, Instant now) {
        String id = "auto:" + key;
        if (messageRepository.existsById(id)) return 0;
        NightMessage m = new NightMessage();
        m.setId(id);
        m.setNight(NightThreadService.nightOf(now));
        m.setMatchId(matchId);
        m.setText(text);
        m.setCreatedAt(now);
        messageRepository.save(m);
        return 1;
    }

    private static boolean periodEnded(LiveStatusDto live) {
        String detail = live.detail() == null ? "" : live.detail().toLowerCase(Locale.ROOT);
        return "0.0".equals(live.clock()) || "0:00".equals(live.clock()) || detail.contains("halftime") || detail.startsWith("end of");
    }

    private static String periodName(int period) {
        if (period <= 4) return "du " + (period == 1 ? "1er" : period + "e") + " quart-temps";
        return period == 5 ? "de la prolongation" : "de la " + (period - 4) + "e prolongation";
    }

    private static String score(Match match, LiveStatusDto live) {
        int away = live.awayScore() == null ? 0 : live.awayScore();
        int home = live.homeScore() == null ? 0 : live.homeScore();
        return match.getAwayTeam().getAbbreviation() + " " + away + " - " + home + " " + match.getHomeTeam().getAbbreviation();
    }
}
