import { apiFetch } from "@/lib/api/http";

/** La semaine de jeu qui vient de finir (WeeklyRecapService). `week` = son lundi, « 2026-11-02 ». */
export interface WeeklyRecap {
  week: string;
  /** Points de classement : bénéfice net, jamais moins de 0. */
  points: number;
  net: number;
  tickets: number;
  won: number;
  rank: number | null;
  players: number;
  champion: boolean;
  best: { betId: string; labels: string; odds: number; profit: number } | null;
  leagues: { id: string; name: string; rank: number; members: number; champion: boolean }[];
  duels: { opponent: string; mine: number; theirs: number; result: "won" | "lost" | "tie" }[];
}

/** null : aucun ticket réglé la semaine passée. */
export async function fetchWeeklyRecap(): Promise<WeeklyRecap | null> {
  return (await apiFetch<WeeklyRecap | undefined>("/weekly-recap")) ?? null;
}
