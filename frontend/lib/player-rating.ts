import type { PlayerBoxScore } from "@/lib/types";

const split = (s: string) => {
  const [made, attempted] = s.split("-").map((n) => Number.parseInt(n, 10) || 0);
  return { made, attempted };
};

// En dessous, la note dirait surtout « pas assez joué ».
const MIN_MINUTES = 5;

/**
 * Note HoopPicks sur 10, d'après la feuille de match. Ce n'est pas la note
 * de Sofascore (leur formule n'est pas publique) : le « Game Score » de John
 * Hollinger, sans rebonds offensifs ni fautes (ESPN ne les détaille pas
 * ici), ramené sur 10 (0 → 6,0 ; 10 → 8,0 ; 20 et plus → 10). Rien sous
 * {@link MIN_MINUTES} minutes jouées.
 */
export function playerRating(p: PlayerBoxScore): number | null {
  if ((Number.parseInt(p.minutes, 10) || 0) < MIN_MINUTES) return null;
  const fg = split(p.fieldGoals);
  const ft = split(p.freeThrows);
  const gameScore =
    p.points + 0.4 * fg.made - 0.7 * fg.attempted - 0.4 * (ft.attempted - ft.made) + 0.5 * p.rebounds +
    p.steals + 0.7 * p.assists + 0.7 * p.blocks - p.turnovers;
  return Math.round(Math.min(10, Math.max(3, 6 + gameScore * 0.2)) * 10) / 10;
}

/** Couleur de pastille, du rouge (match raté) au vert (grand match). */
export function ratingClass(rating: number): string {
  if (rating >= 8) return "bg-success text-white";
  if (rating >= 7) return "bg-success/60 text-white";
  if (rating >= 6) return "bg-amber-500 text-white";
  return "bg-destructive text-white";
}

/** Le mieux noté du match : « l'homme du match ». */
export function bestRated(players: PlayerBoxScore[]): { player: PlayerBoxScore; rating: number } | undefined {
  return players.reduce<{ player: PlayerBoxScore; rating: number } | undefined>((best, p) => {
    const r = playerRating(p);
    return r !== null && (!best || r > best.rating) ? { player: p, rating: r } : best;
  }, undefined);
}
