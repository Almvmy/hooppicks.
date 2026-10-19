package com.hooppicks.backendapplication.bet;

import com.hooppicks.backendapplication.entity.*;
import com.hooppicks.backendapplication.nba.OddsService;
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
 * Paris joueurs : « plus / moins de X » points, rebonds, passes décisives,
 * tirs à 3 points réussis, contres et cumuls (points + rebonds + passes, points
 * + rebonds, points + passes, rebonds + passes, interceptions + contres), pour
 * les trois meilleurs joueurs de chaque équipe dans la statistique, parmi ceux
 * qui jouent au moins 20 minutes. Ligne = moyenne de référence (effectif ESPN), toujours
 * en x,5 : pas d'égalité possible. Cote fixe des deux côtés, comme l'écart et
 * le total.
 *
 * Moyenne de référence en début de saison (choix du propriétaire) : elle part
 * de la saison précédente et glisse vers la saison en cours, 10 % par match
 * joué, puis 100 % saison en cours dès le 10e match. Repartir de zéro donnait
 * des lignes faciles à battre (une star à 12 points sur son premier match
 * aurait eu « plus de 12,5 » au match suivant) ; attendre 10 matchs privait
 * de paris joueurs pendant trois semaines. Sans saison précédente (rookie) :
 * saison en cours seule, à partir de 5 matchs.
 *
 * Les lignes sont recalculées à la volée (pas stockées sur le match) : le
 * serveur les recalcule au moment du pari et refuse si elle a bougé depuis
 * l'affichage, puis la fige sur la sélection.
 */
@Service
public class PlayerPropsService {

    /** Premier marché, gardé sous ce nom : les tickets déjà posés le portent. */
    public static final String MARKET = "player_points";
    public static final double ODDS = OddsService.EVEN_ODDS;
    static final int PLAYERS_PER_TEAM = 3;
    // Rotation : un joueur à moins de 20 minutes a des lignes trop aléatoires.
    static final double MIN_MINUTES = 20;
    // Moyenne fiable : au moins quelques matchs joués. C'est aussi la durée du
    // glissement de la saison précédente vers la saison en cours.
    static final int MIN_GAMES = 10;
    // Sans saison précédente (rookie) : saison en cours seule, à partir de là.
    static final int ROOKIE_MIN_GAMES = 5;
    // Feuille de match ESPN absente après ce délai : on rembourse la sélection
    // plutôt que de bloquer le ticket indéfiniment.
    static final Duration STATS_GRACE = Duration.ofHours(18);

    /**
     * Statistiques proposées. Le plancher de moyenne écarte les lignes sans
     * intérêt (« plus de 1,5 passe » pour un pivot remplaçant) : seuls les
     * joueurs dont c'est vraiment le rôle ont une ligne en rebonds, passes
     * ou tirs à 3 points.
     */
    public enum Stat {
        POINTS("player_points", "pts", 0) {
            Double current(RosterPlayer p) { return p.getPointsPerGame(); }
            Double previous(RosterPlayer p) { return p.getPrevPointsPerGame(); }
            int value(PlayerMatchStat s) { return s.getPoints(); }
        },
        REBOUNDS("player_rebounds", "rbd", 5) {
            Double current(RosterPlayer p) { return p.getReboundsPerGame(); }
            Double previous(RosterPlayer p) { return p.getPrevReboundsPerGame(); }
            int value(PlayerMatchStat s) { return s.getRebounds(); }
        },
        ASSISTS("player_assists", "pd", 4) {
            Double current(RosterPlayer p) { return p.getAssistsPerGame(); }
            Double previous(RosterPlayer p) { return p.getPrevAssistsPerGame(); }
            int value(PlayerMatchStat s) { return s.getAssists(); }
        },
        THREES("player_threes", "tirs à 3 pts", 2) {
            Double current(RosterPlayer p) { return p.getThreePointersMadePerGame(); }
            Double previous(RosterPlayer p) { return p.getPrevThreePointersMadePerGame(); }
            int value(PlayerMatchStat s) { return s.getThreePointsMade(); }
        },
        POINTS_REBOUNDS_ASSISTS("player_pra", "pts+rbd+pd", 0) {
            Double current(RosterPlayer p) {
                return sum(p.getPointsPerGame(), p.getReboundsPerGame(), p.getAssistsPerGame());
            }
            Double previous(RosterPlayer p) {
                return sum(p.getPrevPointsPerGame(), p.getPrevReboundsPerGame(), p.getPrevAssistsPerGame());
            }
            int value(PlayerMatchStat s) { return s.getPoints() + s.getRebounds() + s.getAssists(); }
        },
        POINTS_REBOUNDS("player_pr", "pts+rbd", 0) {
            Double current(RosterPlayer p) { return sum(p.getPointsPerGame(), p.getReboundsPerGame(), 0.0); }
            Double previous(RosterPlayer p) { return sum(p.getPrevPointsPerGame(), p.getPrevReboundsPerGame(), 0.0); }
            int value(PlayerMatchStat s) { return s.getPoints() + s.getRebounds(); }
        },
        POINTS_ASSISTS("player_pa", "pts+pd", 0) {
            Double current(RosterPlayer p) { return sum(p.getPointsPerGame(), p.getAssistsPerGame(), 0.0); }
            Double previous(RosterPlayer p) { return sum(p.getPrevPointsPerGame(), p.getPrevAssistsPerGame(), 0.0); }
            int value(PlayerMatchStat s) { return s.getPoints() + s.getAssists(); }
        },
        REBOUNDS_ASSISTS("player_ra", "rbd+pd", 8) {
            Double current(RosterPlayer p) { return sum(p.getReboundsPerGame(), p.getAssistsPerGame(), 0.0); }
            Double previous(RosterPlayer p) { return sum(p.getPrevReboundsPerGame(), p.getPrevAssistsPerGame(), 0.0); }
            int value(PlayerMatchStat s) { return s.getRebounds() + s.getAssists(); }
        },
        STEALS_BLOCKS("player_stocks", "int+ct", 2) {
            Double current(RosterPlayer p) { return sum(p.getStealsPerGame(), p.getBlocksPerGame(), 0.0); }
            Double previous(RosterPlayer p) { return sum(p.getPrevStealsPerGame(), p.getPrevBlocksPerGame(), 0.0); }
            int value(PlayerMatchStat s) { return s.getSteals() + s.getBlocks(); }
        },
        BLOCKS("player_blocks", "ct", 1.5) {
            Double current(RosterPlayer p) { return p.getBlocksPerGame(); }
            Double previous(RosterPlayer p) { return p.getPrevBlocksPerGame(); }
            int value(PlayerMatchStat s) { return s.getBlocks(); }
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

        /** Moyenne de la saison en cours chez ESPN. */
        abstract Double current(RosterPlayer p);
        /** Moyenne de la saison précédente (null si inconnue). */
        abstract Double previous(RosterPlayer p);
        abstract int value(PlayerMatchStat s);

        /** Moyenne de référence de la ligne (cf. la classe), null si le joueur n'a pas de ligne. */
        Double average(RosterPlayer p) {
            int games = p.getGamesPlayed() == null ? 0 : p.getGamesPlayed();
            Double now = current(p);
            if (games >= MIN_GAMES) return now;
            Double before = previous(p);
            boolean hasPrevious = before != null && p.getPrevGamesPlayed() != null && p.getPrevGamesPlayed() >= MIN_GAMES;
            if (!hasPrevious) return games >= ROOKIE_MIN_GAMES ? now : null;
            if (games == 0 || now == null) return before;
            double weight = (double) games / MIN_GAMES;
            return (1 - weight) * before + weight * now;
        }

        private static Double sum(Double a, Double b, Double c) {
            return a == null || b == null || c == null ? null : a + b + c;
        }

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
                             String headshotUrl, double line, double overOdds, double underOdds, double average,
                             /** « Day-To-Day », « Questionable »… (jamais « Out » : pas de ligne). */
                             String injuryStatus) {}

    /** Paris joueurs d'un match, tous marchés (vide en présaison : les titulaires y jouent peu). */
    public List<PlayerProp> propsFor(Match match) {
        if (match == null || match.getType() == MatchType.PRESEASON) return List.of();
        List<Team> teams = List.of(match.getAwayTeam(), match.getHomeTeam());
        List<List<RosterPlayer>> rosters = teams.stream()
                .map(team -> rosterRepository.findByTeamIdOrderByLastNameAsc(team.getId()).stream()
                        .filter(p -> !"Out".equalsIgnoreCase(p.getInjuryStatus()))
                        .filter(p -> minutes(p) != null && minutes(p) >= MIN_MINUTES)
                        .toList())
                .toList();
        List<PlayerProp> props = new ArrayList<>();
        for (Stat stat : Stat.values()) {
            for (int i = 0; i < teams.size(); i++) {
                String abbreviation = teams.get(i).getAbbreviation();
                rosters.get(i).stream()
                        // Pas de moyenne de référence (rookie à moins de 5 matchs, petit échantillon) : pas de ligne.
                        .filter(p -> stat.average(p) != null && stat.average(p) >= stat.minAverage)
                        .sorted(Comparator.comparingDouble((RosterPlayer p) -> stat.average(p)).reversed())
                        .limit(PLAYERS_PER_TEAM)
                        .map(p -> new PlayerProp(stat.market, p.getId(), p.getFirstName() + " " + p.getLastName(),
                                abbreviation, p.getHeadshotUrl(), line(stat.average(p)), ODDS, ODDS, stat.average(p),
                                p.getInjuryStatus()))
                        .forEach(props::add);
            }
        }
        return props;
    }

    /** Minutes par match de référence : même glissement de saison que les statistiques. */
    static Double minutes(RosterPlayer p) {
        int games = p.getGamesPlayed() == null ? 0 : p.getGamesPlayed();
        Double now = p.getMinutesPerGame();
        if (games >= MIN_GAMES) return now;
        Double before = p.getPrevMinutesPerGame();
        boolean hasPrevious = before != null && p.getPrevGamesPlayed() != null && p.getPrevGamesPlayed() >= MIN_GAMES;
        if (!hasPrevious) return games >= ROOKIE_MIN_GAMES ? now : null;
        if (games == 0 || now == null) return before;
        double weight = (double) games / MIN_GAMES;
        return (1 - weight) * before + weight * now;
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
