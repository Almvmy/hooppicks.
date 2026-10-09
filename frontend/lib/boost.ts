import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api/http";
import type { BetSelection } from "@/lib/types";

/**
 * Cote boostée de la soirée (BoostService) : pari « vainqueur » d'un match
 * payé +15 %, ticket plafonné. Le serveur applique la cote lui-même à la
 * pose : ce qui suit sert à l'afficher.
 */
export interface Boost {
  matchId: string;
  factor: number;
  maxStake: number;
  homeOdds: number;
  awayOdds: number;
}

export function useBoost(): Boost | null | undefined {
  const { data } = useQuery({
    queryKey: ["boost"],
    queryFn: async () => (await apiFetch<Boost | undefined>("/boost")) ?? null,
    staleTime: 5 * 60 * 1000,
  });
  return data;
}

/** Sélection boostée : le pari « vainqueur » du match de la soirée. */
export function isBoostedSelection(selection: Pick<BetSelection, "matchId" | "market">, boost: Boost | null | undefined) {
  return !!boost && selection.market === "moneyline" && selection.matchId === boost.matchId;
}
