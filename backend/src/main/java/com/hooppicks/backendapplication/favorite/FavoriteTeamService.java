package com.hooppicks.backendapplication.favorite;

import com.hooppicks.backendapplication.bet.LegEvaluator;
import com.hooppicks.backendapplication.bet.LegEvaluator.LegResult;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.BetSelection;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tout ce qui se calcule autour de l'équipe favorite d'un joueur : ses
 * paris pour / contre elle (statistiques, badges). L'équipe favorite est
 * stockée en nom complet ("New York Knicks"), Team n'a que le surnom
 * ("Knicks") : on les relie par la fin du nom, comme le frontend.
 */
@Service
public class FavoriteTeamService {

    public enum Side { FOR, AGAINST, NEUTRAL }

    /** Paris d'un joueur vus depuis son équipe favorite, sélection par sélection. */
    public record Stats(String teamAbbreviation,
                        int forLegs, int forWon, int forLost,
                        int againstLegs, int againstWon, int againstLost) {
        static Stats none() {
            return new Stats(null, 0, 0, 0, 0, 0, 0);
        }
    }

    /** Ce dont les badges liés à l'équipe favorite ont besoin. */
    public record BadgeFacts(int forSelections, boolean wonTicketAgainst) {
        public static final BadgeFacts NONE = new BadgeFacts(0, false);
    }

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;

    public FavoriteTeamService(TeamRepository teamRepository, MatchRepository matchRepository) {
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
    }

    public Optional<Team> resolve(String favoriteTeam) {
        if (favoriteTeam == null || favoriteTeam.isBlank()) return Optional.empty();
        return teamRepository.findAll().stream()
                .filter(t -> t.getName() != null && matchesTeam(favoriteTeam, t.getName()))
                .findFirst();
    }

    /** "Charlotte Hornets" correspond à "Hornets", pas à "Nets". */
    public static boolean matchesTeam(String favoriteTeam, String teamName) {
        return favoriteTeam.equals(teamName) || favoriteTeam.endsWith(" " + teamName);
    }

    /**
     * Pour ou contre l'équipe : victoire / handicap du côté de l'équipe =
     * pour, du côté adverse = contre. Un total (plus / moins de points)
     * ne prend parti pour personne.
     */
    public static Side side(BetSelection selection, Match match, String favoriteAbbreviation) {
        if (match == null || favoriteAbbreviation == null || "total".equals(selection.getMarket())) return Side.NEUTRAL;
        boolean favoriteIsHome = favoriteAbbreviation.equals(match.getHomeTeam().getAbbreviation());
        boolean favoriteIsAway = favoriteAbbreviation.equals(match.getAwayTeam().getAbbreviation());
        if (!favoriteIsHome && !favoriteIsAway) return Side.NEUTRAL;
        boolean pickedHome = "home".equals(selection.getOutcome());
        return pickedHome == favoriteIsHome ? Side.FOR : Side.AGAINST;
    }

    @Transactional(readOnly = true)
    public Stats stats(String favoriteTeam, List<Bet> bets) {
        Optional<Team> team = resolve(favoriteTeam);
        if (team.isEmpty()) return Stats.none();
        String abbr = team.get().getAbbreviation();
        Map<String, Match> matches = matchesOf(bets);

        int forLegs = 0, forWon = 0, forLost = 0, againstLegs = 0, againstWon = 0, againstLost = 0;
        for (Bet bet : bets) {
            for (BetSelection s : bet.getSelections()) {
                Match match = matches.get(s.getMatchId());
                Side side = side(s, match, abbr);
                if (side == Side.NEUTRAL) continue;
                LegResult result = match.getStatus() == MatchStatus.FINISHED ? LegEvaluator.evaluate(s, match) : null;
                if (side == Side.FOR) {
                    forLegs++;
                    if (result == LegResult.WIN) forWon++;
                    if (result == LegResult.LOSE) forLost++;
                } else {
                    againstLegs++;
                    if (result == LegResult.WIN) againstWon++;
                    if (result == LegResult.LOSE) againstLost++;
                }
            }
        }
        return new Stats(abbr, forLegs, forWon, forLost, againstLegs, againstWon, againstLost);
    }

    @Transactional(readOnly = true)
    public BadgeFacts badgeFacts(String favoriteTeam, List<Bet> bets) {
        Optional<Team> team = resolve(favoriteTeam);
        if (team.isEmpty()) return BadgeFacts.NONE;
        String abbr = team.get().getAbbreviation();
        Map<String, Match> matches = matchesOf(bets);

        int forSelections = 0;
        boolean wonAgainst = false;
        for (Bet bet : bets) {
            for (BetSelection s : bet.getSelections()) {
                Side side = side(s, matches.get(s.getMatchId()), abbr);
                if (side == Side.FOR) forSelections++;
                if (side == Side.AGAINST && bet.getStatus() == BetStatus.WON) wonAgainst = true;
            }
        }
        return new BadgeFacts(forSelections, wonAgainst);
    }

    private Map<String, Match> matchesOf(List<Bet> bets) {
        List<String> ids = bets.stream()
                .flatMap(b -> b.getSelections().stream())
                .map(BetSelection::getMatchId)
                .distinct()
                .toList();
        if (ids.isEmpty()) return Map.of();
        return matchRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Match::getId, Function.identity()));
    }
}
