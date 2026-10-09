import type { Boost } from "@/lib/boost";
import type { BetSelection, Match } from "@/lib/types";
import { isBettable } from "@/lib/utils";

export interface SuggestedParlay {
  key: "safe" | "bold";
  title: string;
  description: string;
  selections: BetSelection[];
  totalOdds: number;
}

// Fenêtre : la soirée qui vient (un peu plus de 24 h, pour couvrir les matchs
// tardifs d'Asie comme les premiers de la côte Ouest).
const WINDOW_MS = 30 * 60 * 60 * 1000;

function moneyline(match: Match, outcome: "home" | "away", boost: Boost | null | undefined): BetSelection {
  const team = outcome === "home" ? match.homeTeam : match.awayTeam;
  const boosted = boost?.matchId === match.id;
  return {
    id: `${match.id}-moneyline-${outcome}`,
    matchId: match.id,
    matchLabel: `${match.awayTeam.name} vs ${match.homeTeam.name}`,
    market: "moneyline",
    outcome,
    label: `${team.abbreviation} (V)`,
    odds: outcome === "home"
      ? boosted ? boost!.homeOdds : match.odds.moneylineHome
      : boosted ? boost!.awayOdds : match.odds.moneylineAway,
  };
}

/**
 * Deux combinés prêts à jouer pour la soirée, à partir des cotes : « le sûr »
 * (les deux favoris les plus nets) et « le coup » (trois matchs ouverts, du
 * côté de l'outsider). Que des paris « vainqueur » : simples à comprendre.
 * Vide s'il n'y a pas assez de matchs ouverts aux paris.
 */
export function suggestParlays(matches: Match[] | undefined, boost: Boost | null | undefined, now = Date.now()): SuggestedParlay[] {
  const open = (matches ?? []).filter(
    (m) => isBettable(m) && new Date(m.date).getTime() - now < WINDOW_MS && m.odds.moneylineHome > 1 && m.odds.moneylineAway > 1
  );

  const favorites = open
    .map((m) => moneyline(m, m.odds.moneylineHome <= m.odds.moneylineAway ? "home" : "away", boost))
    .filter((s) => s.odds >= 1.2 && s.odds <= 1.7)
    .sort((a, b) => a.odds - b.odds)
    .slice(0, 2);

  const outsiders = open
    .map((m) => moneyline(m, m.odds.moneylineHome > m.odds.moneylineAway ? "home" : "away", boost))
    .filter((s) => s.odds >= 1.9 && s.odds <= 3.2)
    .sort((a, b) => a.odds - b.odds)
    .slice(0, 3);

  const parlays: SuggestedParlay[] = [];
  const total = (sels: BetSelection[]) => sels.reduce((acc, s) => acc * s.odds, 1);
  if (favorites.length === 2) {
    parlays.push({
      key: "safe",
      title: "Le sûr",
      description: "Les deux favoris les plus nets de la soirée.",
      selections: favorites,
      totalOdds: total(favorites),
    });
  }
  if (outsiders.length === 3) {
    parlays.push({
      key: "bold",
      title: "Le coup",
      description: "Trois outsiders qui ont leur chance.",
      selections: outsiders,
      totalOdds: total(outsiders),
    });
  }
  return parlays;
}
