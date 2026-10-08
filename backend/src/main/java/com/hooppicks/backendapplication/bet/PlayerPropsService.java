package com.hooppicks.backendapplication.bet;

import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.PlayerMatchStatRepository;
import com.hooppicks.backendapplication.repository.RosterPlayerRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Paris joueurs : « plus / moins de X points » pour les deux meilleurs
 * marqueurs de chaque équipe. Ligne = moyenne de points de la saison
 * (effectif ESPN), toujours en x,5 : pas d'égalité possible. Cote fixe des
 * deux côtés, comme l'écart et le total.
 *
 * Les lignes sont recalculées à la volée (pas stockées sur le match) : le
 * serveur les recalcule au moment du pari et refuse si elle a bougé depuis
 * l'affichage, puis la fige sur la sélection.
 */
@Service
public class PlayerPropsService {

    public static final String MARKET = "player_points";
    public static final double ODDS = 1.91;
    static final int PLAYERS_PER_TEAM = 2;
    // Moyenne fiable : au moins quelques matchs joués.
    static final int MIN_GAMES = 10;
    // Feuille de match ESPN absente après ce délai : on rembourse la sélection
    // plutôt que de bloquer le ticket indéfiniment.
    static final Duration STATS_GRACE = Duration.ofHours(18);

    private final RosterPlayerRepository rosterRepository;
    private final PlayerMatchStatRepository statRepository;

    public PlayerPropsService(RosterPlayerRepository rosterRepository, PlayerMatchStatRepository statRepository) {
        this.rosterRepository = rosterRepository;
        this.statRepository = statRepository;
    }

    public record PlayerProp(String playerId, String playerName, String teamAbbreviation, String headshotUrl,
                             double line, double overOdds, double underOdds, double pointsPerGame) {}

    /** Paris joueurs d'un match (vide en présaison : les titulaires y jouent peu). */
    public List<PlayerProp> propsFor(Match match) {
        if (match == null || match.getType() == MatchType.PRESEASON) return List.of();
        return Stream.of(match.getAwayTeam(), match.getHomeTeam())
                .flatMap(team -> rosterRepository.findByTeamIdOrderByLastNameAsc(team.getId()).stream()
                        .filter(p -> p.getPointsPerGame() != null && p.getGamesPlayed() != null && p.getGamesPlayed() >= MIN_GAMES)
                        .filter(p -> !"Out".equalsIgnoreCase(p.getInjuryStatus()))
                        .sorted(Comparator.comparingDouble(RosterPlayer::getPointsPerGame).reversed())
                        .limit(PLAYERS_PER_TEAM)
                        .map(p -> new PlayerProp(p.getId(), p.getFirstName() + " " + p.getLastName(), team.getAbbreviation(),
                                p.getHeadshotUrl(), line(p.getPointsPerGame()), ODDS, ODDS, p.getPointsPerGame())))
                .toList();
    }

    public Optional<PlayerProp> find(Match match, String playerId) {
        return propsFor(match).stream().filter(p -> p.playerId().equals(playerId)).findFirst();
    }

    /** 26,2 → 26,5 ; 26,8 → 26,5 ; 27,0 → 27,5 : partie entière + 0,5, jamais d'égalité possible. */
    static double line(double average) {
        return Math.floor(average) + 0.5;
    }

    /**
     * Résultat d'une sélection joueur sur un match terminé, d'après la feuille
     * de match. Vide tant que la feuille n'est pas synchronisée (elle arrive
     * par lots après le match). Joueur absent de la feuille ou sans minutes :
     * remboursé, comme un bookmaker quand le joueur ne joue pas.
     */
    public Optional<LegEvaluator.LegResult> evaluate(BetSelection selection, Match match, Instant now) {
        List<PlayerMatchStat> sheet = statRepository.findByMatchIdOrderByPointsDesc(match.getId());
        if (sheet.isEmpty()) {
            boolean overdue = match.getDate() != null && now.isAfter(match.getDate().plus(STATS_GRACE));
            return overdue ? Optional.of(LegEvaluator.LegResult.PUSH) : Optional.empty();
        }
        String wanted = normalize(selection.getPlayerName());
        Optional<PlayerMatchStat> stat = sheet.stream().filter(s -> normalize(s.getPlayerName()).equals(wanted)).findFirst();
        if (stat.isEmpty() || !played(stat.get())) return Optional.of(LegEvaluator.LegResult.PUSH);
        boolean over = stat.get().getPoints() > selection.getPropLine();
        return Optional.of(over == "over".equals(selection.getOutcome()) ? LegEvaluator.LegResult.WIN : LegEvaluator.LegResult.LOSE);
    }

    private static boolean played(PlayerMatchStat s) {
        String m = s.getMinutes();
        return m != null && !m.isBlank() && !m.startsWith("-") && !"0".equals(m.trim());
    }

    /** « Luka Dončić » et « Luka Doncic », « Jr. » et « Jr » : même joueur. */
    static String normalize(String name) {
        if (name == null) return "";
        String ascii = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z ]", "").replaceAll("\\s+", " ").trim();
    }

    public static boolean isProp(BetSelection selection) {
        return MARKET.equals(selection.getMarket());
    }
}
