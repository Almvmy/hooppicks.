import { apiFetch } from "@/lib/api/http";
import { LeaderboardPhase, Palmares, PhaseChampions, WeekChampions } from "@/lib/types";

/** Champion(s) de la semaine passée, de tout HoopPicks ou d'une ligue. */
export async function fetchWeekChampions(leagueId?: string): Promise<WeekChampions> {
  return apiFetch<WeekChampions>(`/palmares/champions${leagueId ? `?leagueId=${encodeURIComponent(leagueId)}` : ""}`);
}

/** Champion(s) de la Coupe NBA ou des playoffs de la saison, de tout HoopPicks ou d'une ligue. */
export async function fetchPhaseChampions(phase: LeaderboardPhase, leagueId?: string): Promise<PhaseChampions> {
  const league = leagueId ? `&leagueId=${encodeURIComponent(leagueId)}` : "";
  return apiFetch<PhaseChampions>(`/palmares/champions?phase=${phase}${league}`);
}

export async function fetchMyPalmares(): Promise<Palmares> {
  return apiFetch<Palmares>("/palmares/me");
}
