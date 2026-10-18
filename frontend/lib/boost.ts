import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api/http";
import type { BetSelection } from "@/lib/types";

/**
 * Cote boostée de la soirée (BoostService) : pari « vainqueur » d'un match
 * payé +15 %, ticket plafonné. Le serveur applique la cote lui-même à la
 * pose : ce qui suit sert à l'afficher.
 */
export interface BoostedOdds {
  matchId: string;
  homeOdds: number;
  awayOdds: number;
}

export interface Boost {
  /** Le choc de la soirée, null s'il n'y en a pas (soirée d'ouverture annoncée à l'avance). */
  matchId: string | null;
  factor: number;
  maxStake: number;
  homeOdds: number | null;
  awayOdds: number | null;
  /** Soirée d'ouverture de la saison à venir ou en cours : tous ses matchs sont boostés. */
  openingNight: boolean;
  /** Tous les matchs boostés, choc compris. */
  matches: BoostedOdds[];
}

/** Cotes « vainqueur » boostées de ce match, s'il l'est. */
export function boostedOdds(boost: Boost | null | undefined, matchId: string): BoostedOdds | null {
  return boost?.matches?.find((m) => m.matchId === matchId) ?? null;
}

export function useBoost(): Boost | null | undefined {
  const { data } = useQuery({
    queryKey: ["boost"],
    queryFn: async () => (await apiFetch<Boost | undefined>("/boost")) ?? null,
    staleTime: 5 * 60 * 1000,
  });
  return data;
}

/** Sélection boostée : le pari « vainqueur » d'un match boosté. */
export function isBoostedSelection(selection: Pick<BetSelection, "matchId" | "market">, boost: Boost | null | undefined) {
  return selection.market === "moneyline" && boostedOdds(boost, selection.matchId) !== null;
}
