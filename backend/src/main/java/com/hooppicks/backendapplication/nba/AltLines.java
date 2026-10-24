package com.hooppicks.backendapplication.nba;

import com.hooppicks.backendapplication.entity.Match;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handicaps et totaux alternatifs : plusieurs lignes par match, chacune avec
 * sa propre cote, autour de la ligne principale (qui reste à 1,96). Le joueur
 * choisit son risque : DET −10,5 paie bien plus que DET −4,5, DET +1,5 bien
 * moins.
 *
 * Écart final et total de points modélisés par une loi normale centrée sur
 * les lignes du match (déjà calculées par OddsService à partir de l'Elo et de
 * la forme) : écart-type de 13 points pour l'écart, 18 pour le total, ordres
 * de grandeur NBA. Même marge de 2 % et même plancher que les autres cotes.
 * Calculées à la volée à partir des lignes figées du match : elles ne bougent
 * pas plus que lui (cotes fixes), et placeBet les recalcule pour ne jamais
 * reprendre une cote envoyée par le client.
 *
 * Lignes toujours en x,5 (jamais d'égalité) ; handicap exprimé côté domicile,
 * comme `Match.spreadValue` : −7,5 = l'équipe à domicile doit gagner de 8.
 */
public final class AltLines {

    public static final String ALT_SPREAD = "alt_spread";
    public static final String ALT_TOTAL = "alt_total";

    static final double MARGIN_SD = 13.0;
    static final double TOTAL_SD = 18.0;
    private static final double VIG = 1.02;
    private static final double MIN_ODDS = 1.03;
    private static final double MAX_ODDS = 15.0;
    private static final double[] SPREAD_STEPS = {-6, -3, 3, 6};
    private static final double[] TOTAL_STEPS = {-10, -5, 5, 10};

    /** Une ligne et la cote de chaque côté (domicile/extérieur ou plus/moins). */
    public record Line(String market, double line, double firstOdds, double secondOdds) {}

    private AltLines() {
    }

    public static List<Line> of(Match match) {
        List<Line> lines = new ArrayList<>();
        // Cotes pas encore posées (total à 0) : rien à proposer.
        if (match.getTotalValue() <= 0) return lines;
        {
            double base = half(match.getSpreadValue());
            for (double step : SPREAD_STEPS) {
                double line = base + step;
                lines.add(new Line(ALT_SPREAD, line, odds(match, ALT_SPREAD, "home", line), odds(match, ALT_SPREAD, "away", line)));
            }
        }
        {
            double base = half(match.getTotalValue());
            for (double step : TOTAL_STEPS) {
                double line = base + step;
                lines.add(new Line(ALT_TOTAL, line, odds(match, ALT_TOTAL, "over", line), odds(match, ALT_TOTAL, "under", line)));
            }
        }
        return lines;
    }

    /** Cote d'une ligne proposée pour ce match, vide si la ligne n'est pas proposée (ou plus). */
    public static Optional<Double> find(Match match, String market, String outcome, Double line) {
        if (line == null) return Optional.empty();
        return of(match).stream()
                .filter(l -> l.market().equals(market) && Math.abs(l.line() - line) < 0.01)
                .findFirst()
                .map(l -> "home".equals(outcome) || "over".equals(outcome) ? l.firstOdds() : l.secondOdds());
    }

    static double odds(Match match, String market, String outcome, double line) {
        double p;
        if (ALT_SPREAD.equals(market)) {
            // Écart attendu (domicile − extérieur) = −spreadValue ; le domicile
            // couvre la ligne L si l'écart dépasse −L.
            double expectedMargin = -match.getSpreadValue();
            double homeCovers = 1 - normalCdf((-line - expectedMargin) / MARGIN_SD);
            p = "home".equals(outcome) ? homeCovers : 1 - homeCovers;
        } else {
            double over = 1 - normalCdf((line - match.getTotalValue()) / TOTAL_SD);
            p = "over".equals(outcome) ? over : 1 - over;
        }
        return Math.max(MIN_ODDS, Math.min(MAX_ODDS, Math.round(100.0 / (Math.max(p, 0.01) * VIG)) / 100.0));
    }

    // Une ligne entière (−4) passe à la demi-ligne inférieure (−4,5) : pas d'égalité possible.
    private static double half(double value) {
        return value == Math.rint(value) ? value - 0.5 : value;
    }

    /** Fonction de répartition de la loi normale centrée réduite (approximation d'Abramowitz et Stegun, erreur < 1e-7). */
    public static double normalCdf(double x) {
        double t = 1 / (1 + 0.2316419 * Math.abs(x));
        double d = 0.3989422804014327 * Math.exp(-x * x / 2);
        double tail = d * t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))));
        return x >= 0 ? 1 - tail : tail;
    }
}
