import type { BetMarket, PlayerBoxScore, PlayerPropMarket } from "@/lib/types";

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

/** Statistique d'un joueur sur une ligne de feuille de match, selon le marché (même règle que PlayerPropsService.Stat). */
export function propStatValue(market: PlayerPropMarket, p: PlayerBoxScore): number {
  switch (market) {
    case "player_points":
      return p.points;
    case "player_rebounds":
      return p.rebounds;
    case "player_assists":
      return p.assists;
    case "player_threes":
      return Number.parseInt(p.threePoints.split("-")[0], 10) || 0;
    case "player_pra":
      return p.points + p.rebounds + p.assists;
  }
}

/** « Luka Dončić » et « Luka Doncic », « Jr. » et « Jr » : même joueur (comme PlayerPropsService.normalize). */
export function normalizePlayerName(name: string): string {
  return name
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()
    .replace(/[^a-z ]/g, "")
    .replace(/\s+/g, " ")
    .trim();
}

export function isPlayerPropMarket(market: BetMarket): market is PlayerPropMarket {
  return market in PLAYER_PROP_MARKETS;
}
