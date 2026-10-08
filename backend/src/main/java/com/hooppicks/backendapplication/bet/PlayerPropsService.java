package com.hooppicks.backendapplication.bet;

import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.repository.PlayerMatchStatRepository;
import com.hooppicks.backendapplication.repository.RosterPlayerRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Paris joueurs : « plus / moins de X » points, rebonds, passes décisives ou
 * points + rebonds + passes, pour les deux meilleurs joueurs de chaque équipe
 * dans la statistique. Ligne = moyenne de la saison (effectif ESPN), toujours
 * en x,5 : pas d'égalité possible. Cote fixe des deux côtés, comme l'écart et
 * le total.
 *
 * Les lignes sont recalculées à la volée (pas stockées sur le match) : le
 * serveur les recalcule au moment du pari et refuse si elle a bougé depuis
 * l'affichage, puis la fige sur la sélection.
 */
@Service
public class PlayerPropsService {

    /** Premier marché, gardé sous ce nom : les tickets déjà posés le portent. */
    public static final String MARKET = "player_points";
    public static final double ODDS = 1.91;
    static final int PLAYERS_PER_TEAM = 2;
    // Moyenne fiable : au moins quelques matchs joués.
    static final int MIN_GAMES = 10;
    // Feuille de match ESPN absente après ce délai : on rembourse la sélection
    // plutôt que de bloquer le ticket indéfiniment.
    static final Duration STATS_GRACE = Duration.ofHours(18);

    /**
     * Statistiques proposées. Le plancher de moyenne écarte les lignes sans
     * intérêt (« plus de 1,5 passe » pour un pivot remplaçant) : seuls les
     * joueurs dont c'est vraiment le rôle ont une ligne en rebonds ou passes.
     */
    public enum Stat {
        POINTS("player_points", "pts", 0) {
            Double average(RosterPlayer p) { return p.getPointsPerGame(); }
            int value(PlayerMatchStat s) { return s.getPoints(); }
        },
        REBOUNDS("player_rebounds", "rbd", 5) {
            Double average(RosterPlayer p) { return p.getReboundsPerGame(); }
            int value(PlayerMatchStat s) { return s.getRebounds(); }
        },
        ASSISTS("player_assists", "pd", 4) {
            Double average(RosterPlayer p) { return p.getAssistsPerGame(); }
            int value(PlayerMatchStat s) { return s.getAssists(); }
        },
        POINTS_REBOUNDS_ASSISTS("player_pra", "pts+rbd+pd", 0) {
            Double average(RosterPlayer p) {
                if (p.getPointsPerGame() == null || p.getReboundsPerGame() == null || p.getAssistsPerGame() == null) return null;
                return p.getPointsPerGame() + p.getReboundsPerGame() + p.getAssistsPerGame();
            }
            int value(PlayerMatchStat s) { return s.getPoints() + s.getRebounds() + s.getAssists(); }
        };

        public final String market;
        /** Unité des libellés : « Plus de 8,5 rbd ». */
        public final String unit;
        final double minAverage;

        Stat(String market, String unit, double minAverage) {
            this.market = market;
            this.unit = unit;
            this.minAverage = minAverage;
        }

        abstract Double average(RosterPlayer p);
        abstract int value(PlayerMatchStat s);

        public static Optional<Stat> of(String market) {
            return Stream.of(values()).filter(st -> st.market.equals(market)).findFirst();
        }
    }

    private final RosterPlayerRepository rosterRepository;
    private final PlayerMatchStatRepository statRepository;

    public PlayerPropsService(RosterPlayerRepository rosterRepository, PlayerMatchStatRepository statRepository) {
        this.rosterRepository = rosterRepository;
        this.statRepository = statRepository;
    }

    public record PlayerProp(String market, String playerId, String playerName, String teamAbbreviation,
                             String headshotUrl, double line, double overOdds, double underOdds, double average) {}

    /** Paris joueurs d'un match, tous marchés (vide en présaison : les titulaires y jouent peu). */
    public List<PlayerProp> propsFor(Match match) {
        if (match == null || match.getType() == MatchType.PRESEASON) return List.of();
        List<Team> teams = List.of(match.getAwayTeam(), match.getHomeTeam());
        List<List<RosterPlayer>> rosters = teams.stream()
                .map(team -> rosterRepository.findByTeamIdOrderByLastNameAsc(team.getId()).stream()
                        .filter(p -> p.getGamesPlayed() != null && p.getGamesPlayed() >= MIN_GAMES)
                        .filter(p -> !"Out".equalsIgnoreCase(p.getInjuryStatus()))
                        .toList())
                .toList();
        List<PlayerProp> props = new ArrayList<>();
        for (Stat stat : Stat.values()) {
            for (int i = 0; i < teams.size(); i++) {
                String abbreviation = teams.get(i).getAbbreviation();
                rosters.get(i).stream()
                        .filter(p -> stat.average(p) != null && stat.average(p) >= stat.minAverage)
                        .sorted(Comparator.comparingDouble((RosterPlayer p) -> stat.average(p)).reversed())
                        .limit(PLAYERS_PER_TEAM)
                        .map(p -> new PlayerProp(stat.market, p.getId(), p.getFirstName() + " " + p.getLastName(),
                                abbreviation, p.getHeadshotUrl(), line(stat.average(p)), ODDS, ODDS, stat.average(p)))
                        .forEach(props::add);
            }
        }
        return props;
    }

    public Optional<PlayerProp> find(Match match, String market, String playerId) {
        return propsFor(match).stream()
                .filter(p -> p.market().equals(market) && p.playerId().equals(playerId))
                .findFirst();
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
        // Les tickets d'avant l'ajout des autres statistiques n'ont que des points.
        Stat measured = Stat.of(selection.getMarket()).orElse(Stat.POINTS);
        boolean over = measured.value(stat.get()) > selection.getPropLine();
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
        return isPropMarket(selection.getMarket());
    }

    public static boolean isPropMarket(String market) {
        return Stat.of(market).isPresent();
    }
}
