package com.hooppicks.backendapplication.bet;

import com.hooppicks.backendapplication.entity.BetSelection;
import com.hooppicks.backendapplication.entity.Match;

/**
 * Résultat d'une sélection sur un match terminé. Sorti de
 * BetResolutionService pour être partagé avec les statistiques « pour /
 * contre ton équipe » (FavoriteTeamService) : une seule définition de ce
 * qu'est une sélection gagnante, jamais deux qui divergent.
 */
public final class LegEvaluator {

    public enum LegResult { WIN, LOSE, PUSH }

    private LegEvaluator() {
    }

    public static LegResult evaluate(BetSelection selection, Match match) {
        int home = match.getHomeScore() != null ? match.getHomeScore() : 0;
        int away = match.getAwayScore() != null ? match.getAwayScore() : 0;
        boolean selectionIsHome = "home".equals(selection.getOutcome());

        return switch (selection.getMarket()) {
            case "moneyline" -> {
                if (home == away) yield LegResult.PUSH; // n'arrive jamais au basket (pas de match nul), gardé par sécurité
                boolean homeWins = home > away;
                yield (homeWins == selectionIsHome) ? LegResult.WIN : LegResult.LOSE;
            }
            case "spread" -> {
                double homeAdjusted = home + match.getSpreadValue();
                if (homeAdjusted == away) yield LegResult.PUSH;
                boolean homeCovers = homeAdjusted > away;
                yield (homeCovers == selectionIsHome) ? LegResult.WIN : LegResult.LOSE;
            }
            case "total" -> {
                int total = home + away;
                if (total == match.getTotalValue()) yield LegResult.PUSH;
                boolean overWins = total > match.getTotalValue();
                boolean selectionIsOver = "over".equals(selection.getOutcome());
                yield (overWins == selectionIsOver) ? LegResult.WIN : LegResult.LOSE;
            }
            default -> throw new IllegalStateException("Marché inconnu : " + selection.getMarket());
        };
    }
}
