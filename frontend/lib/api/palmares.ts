import { apiFetch } from "@/lib/api/http";
import { Palmares, WeekChampions } from "@/lib/types";

/** Champion(s) de la semaine passée, de tout HoopPicks ou d'une ligue. */
export async function fetchWeekChampions(leagueId?: string): Promise<WeekChampions> {
  return apiFetch<WeekChampions>(`/palmares/champions${leagueId ? `?leagueId=${encodeURIComponent(leagueId)}` : ""}`);
}

export async function fetchMyPalmares(): Promise<Palmares> {
  return apiFetch<Palmares>("/palmares/me");
}
