import { apiFetch } from "@/lib/api/http";
import { FavoriteTeamStats } from "@/lib/types";

export async function fetchFavoriteTeamStats(): Promise<FavoriteTeamStats> {
  return apiFetch<FavoriteTeamStats>("/favorite-team/stats");
}
