package com.hooppicks.backendapplication.bet;

import com.hooppicks.backendapplication.bet.LegEvaluator.LegResult;
import com.hooppicks.backendapplication.entity.BetSelection;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.nba.AltLines;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Ce que vaut un ticket en attente si on l'encaisse maintenant (choix du propriétaire) :
 * gain prévu × probabilité que chaque sélection encore en jeu passe, moins 5 %.
 * Sans calcul en base : tout arrive déjà lu (matchs, direct, résultat des paris joueurs).
 *
 * Probabilité d'une sélection : avant le match, celle de la cote du match (marge de 2 %
 * retirée) ; en direct, la probabilité de victoire d'ESPN pour le vainqueur, le score du
 * moment et le temps restant pour un écart ou un total (même loi normale que les lignes
 * alternatives, resserrée avec le temps qui reste) ; gagnée = 1, remboursée = sa cote
 * retirée du gain ; perdue = plus de cash-out.
 *
 * Les 5 % : le direct d'ESPN a 20 à 60 s de retard, celui qui regarde le match voit le
 * panier avant nous. Encaisser juste après la pose rend donc un peu moins que la mise :
 * on ne pose pas un ticket pour l'annuler gratuitement.
 */
public final class CashOutPricing {

    static final double MARGIN = 0.05;
    private static final double VIG = 1.02;
    private static final double MARGIN_SD = 13.0;
    private static final double TOTAL_SD = 18.0;
    private static final int PERIOD_SECONDS = 720;
    private static final int REGULATION_SECONDS = 4 * PERIOD_SECONDS;
    /** Direct plus vieux que ça : on ne fixe pas de prix sur un score qui n'est peut-être plus le bon. */
    static final Duration MAX_LIVE_AGE = Duration.ofSeconds(60);
    /** Fin de match serrée, même définition que l'alerte des matchs suivis. */
    private static final int CLUTCH_SECONDS = 120;
    private static final int CLUTCH_MARGIN = 5;

    public static final String SUSPENDED_CLUTCH = "Fin de match serrée";
    public static final String SUSPENDED_LIVE = "Direct indisponible";
    public static final String SUSPENDED_PROP = "Pari joueur en cours";
    public static final String SUSPENDED_RESULT = "Résultat en attente";

    private CashOutPricing() {}

    /** Le direct d'un match tel qu'on le connaît, et son âge. */
    public record Live(String state, int period, String clock, int homeScore, int awayScore, Double homeWinPct, Duration age) {}

    /**
     * Une sélection du ticket. `live` : null si le match n'a pas de direct (pas commencé,
     * ou ESPN muet). `propResult` : résultat d'un pari joueur dont le match est fini, vide
     * tant que la feuille de match n'est pas arrivée ; ignoré pour les autres paris.
     */
    public record Leg(BetSelection selection, Match match, Live live, Optional<LegResult> propResult) {}

    /** `suspended` non null : pas d'encaissement pour l'instant, il reviendra. */
    public record Quote(int value, String suspended) {
        public boolean available() {
            return suspended == null && value > 0;
        }
    }

    /** Vide : plus de cash-out du tout (une sélection perdue, ou tout est joué et le ticket va être réglé). */
    public static Optional<Quote> price(int potentialPayout, List<Leg> legs) {
        double factor = 1.0;
        String suspended = null;
        boolean allDecided = true;
        for (Leg leg : legs) {
            BetSelection s = leg.selection();
            Match m = leg.match();
            if (m == null) {
                allDecided = false;
                suspended = first(suspended, SUSPENDED_RESULT);
                continue;
            }
            if (m.getStatus() == MatchStatus.FINISHED) {
                Optional<LegResult> result = PlayerPropsService.isProp(s) ? leg.propResult() : Optional.of(LegEvaluator.evaluate(s, m));
                if (result == null || result.isEmpty()) {
                    allDecided = false;
                    suspended = first(suspended, SUSPENDED_RESULT);
                    continue;
                }
                if (result.get() == LegResult.LOSE) return Optional.empty();
                // Remboursée : sa cote ne compte plus dans le gain, comme au règlement.
                if (result.get() == LegResult.PUSH) factor /= s.getOdds();
                continue;
            }
            allDecided = false;
            Live live = leg.live();
            // ESPN dit « fini » mais notre synchro pas encore : le résultat est connu, pas encore le nôtre.
            if (live != null && "post".equals(live.state())) {
                suspended = first(suspended, SUSPENDED_RESULT);
                continue;
            }
            boolean started = m.getStatus() == MatchStatus.LIVE || (live != null && "in".equals(live.state()));
            if (!started) {
                factor *= pregame(s, m);
                continue;
            }
            if (PlayerPropsService.isProp(s)) {
                suspended = first(suspended, SUSPENDED_PROP);
                continue;
            }
            if (live == null || live.age() == null || live.age().compareTo(MAX_LIVE_AGE) > 0) {
                suspended = first(suspended, SUSPENDED_LIVE);
                continue;
            }
            int remaining = secondsLeft(live.period(), live.clock());
            int margin = live.homeScore() - live.awayScore();
            if (live.period() >= 4 && remaining <= CLUTCH_SECONDS && Math.abs(margin) <= CLUTCH_MARGIN) {
                suspended = first(suspended, SUSPENDED_CLUTCH);
                continue;
            }
            factor *= inPlay(s, m, live, remaining);
        }
        if (allDecided) return Optional.empty();
        int value = (int) Math.floor(potentialPayout * factor * (1 - MARGIN));
        return Optional.of(new Quote(Math.max(0, value), suspended));
    }

    private static String first(String current, String reason) {
        return current != null ? current : reason;
    }

    /** Avant le match : la probabilité de la cote du match, marge retirée (jamais la cote boostée du ticket). */
    static double pregame(BetSelection s, Match m) {
        String market = s.getMarket();
        boolean first = "home".equals(s.getOutcome()) || "over".equals(s.getOutcome());
        double odds = switch (market) {
            case "moneyline" -> first ? m.getMoneylineHome() : m.getMoneylineAway();
            case "spread" -> first ? m.getSpreadOddsHome() : m.getSpreadOddsAway();
            case "total" -> first ? m.getTotalOddsOver() : m.getTotalOddsUnder();
            case AltLines.ALT_SPREAD, AltLines.ALT_TOTAL -> AltLines.find(m, market, s.getOutcome(), s.getPropLine()).orElse(s.getOdds());
            default -> s.getOdds(); // paris joueurs : jamais boostés, la cote du ticket est la bonne
        };
        return clamp(1.0 / (Math.max(odds, 1.01) * VIG));
    }

    /** Pendant le match. `remaining` : secondes de jeu restantes (temps réglementaire, ou prolongation en cours). */
    static double inPlay(BetSelection s, Match m, Live live, int remaining) {
        double f = remaining / (double) REGULATION_SECONDS;
        int margin = live.homeScore() - live.awayScore();
        boolean home = "home".equals(s.getOutcome());
        return switch (s.getMarket()) {
            case "moneyline" -> {
                double pHome = live.homeWinPct() != null ? live.homeWinPct() : above(margin - m.getSpreadValue() * f, 0, MARGIN_SD, f);
                yield clamp(home ? pHome : 1 - pHome);
            }
            case "spread", AltLines.ALT_SPREAD -> {
                double line = "spread".equals(s.getMarket()) ? m.getSpreadValue() : s.getPropLine();
                // Domicile couvre si écart final + ligne > 0 ; écart attendu d'ici la fin = celui d'avant le match, au prorata.
                double pHome = above(margin - m.getSpreadValue() * f, -line, MARGIN_SD, f);
                yield clamp(home ? pHome : 1 - pHome);
            }
            case "total", AltLines.ALT_TOTAL -> {
                double line = "total".equals(s.getMarket()) ? m.getTotalValue() : s.getPropLine();
                double expected = live.homeScore() + live.awayScore() + m.getTotalValue() * f;
                double pOver = above(expected, line, TOTAL_SD, f);
                yield clamp("over".equals(s.getOutcome()) ? pOver : 1 - pOver);
            }
            default -> pregame(s, m);
        };
    }

    /** P(valeur finale > seuil) : loi normale centrée sur l'attendu, écart-type réduit avec le temps restant. */
    private static double above(double expected, double threshold, double sd, double fractionLeft) {
        double spread = sd * Math.sqrt(Math.max(0, fractionLeft));
        if (spread < 1e-6) return expected > threshold ? 1 : 0;
        return 1 - AltLines.normalCdf((threshold - expected) / spread);
    }

    /** « 5:42 », « 42.3 » ou vide. Prolongation (5e période et plus) : seulement ce qui reste d'elle. */
    static int secondsLeft(int period, String clock) {
        int inPeriod = parseClock(clock);
        if (period <= 0) return REGULATION_SECONDS;
        if (period > 4) return inPeriod;
        return (4 - period) * PERIOD_SECONDS + inPeriod;
    }

    private static int parseClock(String clock) {
        if (clock == null || clock.isBlank()) return 0;
        try {
            String c = clock.trim();
            if (c.contains(":")) {
                String[] parts = c.split(":");
                return Integer.parseInt(parts[0]) * 60 + (int) Math.floor(Double.parseDouble(parts[1]));
            }
            return (int) Math.floor(Double.parseDouble(c));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double clamp(double p) {
        return Math.min(0.999, Math.max(0.001, p));
    }
}
