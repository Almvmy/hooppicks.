import type { SeasonQuestion } from "@/lib/api/season-picks";
import type { Conference, TeamRank } from "@/lib/types";

// Les finalistes se choisissent dans leur conférence.
export const CONFERENCE_OF: Partial<Record<SeasonQuestion["key"], Conference>> = {
  east_finalist: "Est",
  west_finalist: "Ouest",
};

// Plus l'épreuve est longue (quatre tours de playoffs pour le titre), plus
// l'écart d'Elo pèse : l'exposant amplifie la différence de niveau.
const EXPONENT: Record<SeasonQuestion["key"], number> = {
  champion: 3,
  east_finalist: 2.5,
  west_finalist: 2.5,
  best_record: 2.5,
};

/**
 * Estimation HoopPicks des chances de chaque équipe pour une question,
 * d'après l'Elo (le même qui fixe les cotes) : pas un calcul de probabilité
 * exact, un repère pour choisir. En pourcentage, de la plus probable à la moins probable.
 */
export function seasonChances(question: SeasonQuestion["key"], teams: TeamRank[]): { team: TeamRank; pct: number }[] {
  const conference = CONFERENCE_OF[question];
  const pool = conference ? teams.filter((t) => t.conference === conference) : teams;
  if (pool.length === 0) return [];
  const weights = pool.map((t) => Math.pow(10, ((t.eloRating - 1500) / 400) * EXPONENT[question]));
  const sum = weights.reduce((a, b) => a + b, 0);
  return pool
    .map((team, i) => ({ team, pct: (weights[i] / sum) * 100 }))
    .sort((a, b) => b.pct - a.pct);
}

export function formatChance(pct: number): string {
  if (pct < 1) return "< 1 %";
  return `${Math.round(pct)} %`;
}
