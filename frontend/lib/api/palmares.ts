import { apiFetch } from "@/lib/api/http";
import { ChampionPhase, Palmares, PhaseChampions, WeekChampions } from "@/lib/types";

/** Champion(s) de la semaine passée, de tout HoopPicks ou d'une ligue. */
export async function fetchWeekChampions(leagueId?: string): Promise<WeekChampions> {
  return apiFetch<WeekChampions>(`/palmares/champions${leagueId ? `?leagueId=${encodeURIComponent(leagueId)}` : ""}`);
}

/** Champion(s) d'une phase de la saison ou du mois passé, de tout HoopPicks ou d'une ligue. */
export async function fetchPhaseChampions(phase: ChampionPhase, leagueId?: string): Promise<PhaseChampions> {
  const league = leagueId ? `&leagueId=${encodeURIComponent(leagueId)}` : "";
  return apiFetch<PhaseChampions>(`/palmares/champions?phase=${phase}${league}`);
}

export async function fetchMyPalmares(): Promise<Palmares> {
  return apiFetch<Palmares>("/palmares/me");
}
