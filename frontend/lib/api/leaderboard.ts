import { apiFetch } from "@/lib/api/http";
import { LeaderboardEntry } from "@/lib/types";

export async function fetchLeaderboard(): Promise<LeaderboardEntry[]> {
  return apiFetch<LeaderboardEntry[]>("/leaderboard");
}