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
