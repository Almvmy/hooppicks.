import type { BetMarket, PlayerPropMarket } from "@/lib/types";

/**
 * Statistiques des paris joueurs : mêmes marchés et mêmes unités que
 * PlayerPropsService.Stat (backend), qui construit les libellés des tickets.
 */
export const PLAYER_PROP_MARKETS: Record<PlayerPropMarket, { tab: string; unit: string }> = {
  player_points: { tab: "Points", unit: "pts" },
  player_rebounds: { tab: "Rebonds", unit: "rbd" },
  player_assists: { tab: "Passes", unit: "pd" },
  player_threes: { tab: "3 pts", unit: "tirs à 3 pts" },
  player_pra: { tab: "Pts+Rbd+Pd", unit: "pts+rbd+pd" },
};

export function isPlayerPropMarket(market: BetMarket): market is PlayerPropMarket {
  return market in PLAYER_PROP_MARKETS;
}
