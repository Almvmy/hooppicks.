import type { BetMarket, BetSelection, Match, PlayerBoxScore, PlayerProp, PlayerPropMarket, PropMilestone } from "@/lib/types";

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
  player_pr: { tab: "Pts+Rbd", unit: "pts+rbd" },
  player_pa: { tab: "Pts+Pd", unit: "pts+pd" },
  player_ra: { tab: "Rbd+Pd", unit: "rbd+pd" },
  player_stocks: { tab: "Int+Contres", unit: "int+ct" },
  player_blocks: { tab: "Contres", unit: "ct" },
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
    case "player_pr":
      return p.points + p.rebounds;
    case "player_pa":
      return p.points + p.assists;
    case "player_ra":
      return p.rebounds + p.assists;
    case "player_stocks":
      return p.steals + p.blocks;
    case "player_blocks":
      return p.blocks;
  }
}

export function shortPlayerName(name: string) {
  const [first, ...rest] = name.split(" ");
  return rest.length ? `${first[0]}. ${rest.join(" ")}` : name;
}

export const decimal = (value: number, digits?: number) =>
  (digits === undefined ? value.toString() : value.toFixed(digits)).replace(".", ",");

/** Sélection d'un pari joueur, libellé identique à celui construit par le serveur (BetController.propLabel). */
export function propSelection(match: Match, prop: PlayerProp, outcome: "over" | "under"): BetSelection {
  const { unit } = PLAYER_PROP_MARKETS[prop.market];
  return {
    id: `${match.id}-${prop.market}-${prop.playerId}-${outcome}`,
    matchId: match.id,
    matchLabel: `${match.awayTeam.name} vs ${match.homeTeam.name}`,
    market: prop.market,
    outcome,
    label: `${shortPlayerName(prop.playerName)} · ${outcome === "over" ? "Plus" : "Moins"} de ${decimal(prop.line)} ${unit}`,
    odds: outcome === "over" ? prop.overOdds : prop.underOdds,
    playerId: prop.playerId,
    line: prop.line,
  };
}

/** Palier « 30+ pts », libellé identique à BetController.milestoneLabel. */
export function milestoneSelection(match: Match, prop: PlayerProp, m: PropMilestone): BetSelection {
  const { unit } = PLAYER_PROP_MARKETS[prop.market];
  return {
    id: `${match.id}-${prop.market}-${prop.playerId}-${m.threshold}plus`,
    matchId: match.id,
    matchLabel: `${match.awayTeam.name} vs ${match.homeTeam.name}`,
    market: prop.market,
    outcome: "over",
    label: `${shortPlayerName(prop.playerName)} · ${m.threshold}+ ${unit}`,
    odds: m.odds,
    playerId: prop.playerId,
    line: m.line,
  };
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
