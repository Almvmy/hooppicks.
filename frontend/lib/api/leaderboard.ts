import { apiFetch } from "@/lib/api/http";
import { LeaderboardEntry, LeaderboardPeriod } from "@/lib/types";

export async function fetchLeaderboard(period: LeaderboardPeriod = "season"): Promise<LeaderboardEntry[]> {
  return apiFetch<LeaderboardEntry[]>(period === "season" ? "/leaderboard" : `/leaderboard?period=${period}`);
}

/**
 * Clé React Query : la saison garde ["leaderboard"], déjà partagée avec le
 * tableau de bord et le profil, pour ne pas la recharger deux fois.
 */
export function leaderboardQueryKey(period: LeaderboardPeriod) {
  return period === "season" ? ["leaderboard"] : ["leaderboard", period];
}

/** Classements Coupe NBA et playoffs proposés : phase commencée cette saison. */
export async function fetchLeaderboardPhases(): Promise<{ cup: boolean; playoffs: boolean }> {
  return apiFetch<{ cup: boolean; playoffs: boolean }>("/leaderboard/phases");
}

export type MyStanding = {
  seasonPoints: number;
  seasonRank: number | null;
  seasonPlayers: number;
  weekPoints: number;
  weekRank: number | null;
  weekPlayers: number;
};

/** Score et rang du joueur connecté : pastille de la barre du haut. */
export async function fetchMyStanding(): Promise<MyStanding> {
  return apiFetch<MyStanding>("/leaderboard/me");
}
