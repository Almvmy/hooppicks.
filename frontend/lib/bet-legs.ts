import type { BetSelection, Match, PlacedBet } from "@/lib/types";

/**
 * État d'une sélection, recalculé côté navigateur pour l'affichage : même
 * règle que BetResolutionService.evaluateSelection (backend), qui reste
 * seul juge pour créditer les points. Pendant un match, on applique la
 * règle au score du moment pour dire « bien parti / mal parti ».
 */
export type LegState = "won" | "lost" | "push" | "winning" | "losing" | "level" | "upcoming" | "unknown";

type LegResult = "win" | "lose" | "push";

function evaluate(selection: Pick<BetSelection, "market" | "outcome">, match: Match): LegResult {
  const home = match.homeScore ?? 0;
  const away = match.awayScore ?? 0;
  const pickedHome = selection.outcome === "home";

  switch (selection.market) {
    case "moneyline": {
      if (home === away) return "push";
      return home > away === pickedHome ? "win" : "lose";
    }
    case "spread": {
      const adjustedHome = home + match.odds.spreadValue;
      if (adjustedHome === away) return "push";
      return adjustedHome > away === pickedHome ? "win" : "lose";
    }
    case "total": {
      const total = home + away;
      if (total === match.odds.totalValue) return "push";
      return total > match.odds.totalValue === (selection.outcome === "over") ? "win" : "lose";
    }
  }
}

export function legState(selection: Pick<BetSelection, "market" | "outcome">, match: Match | undefined): LegState {
  if (!match) return "unknown";
  if (match.status === "scheduled") return "upcoming";
  const result = evaluate(selection, match);
  if (match.status === "finished") return result === "win" ? "won" : result === "lose" ? "lost" : "push";
  return result === "win" ? "winning" : result === "lose" ? "losing" : "level";
}

/** Bilan net d'un ticket résolu : gain moins mise (0 pour un ticket remboursé). */
export function netResult(bet: PlacedBet): number {
  if (bet.status === "won") return bet.potentialPayout - bet.stake;
  if (bet.status === "lost") return -bet.stake;
  return 0;
}
