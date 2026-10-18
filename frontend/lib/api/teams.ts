import { apiFetch } from "@/lib/api/http";
import { RosterPlayer, TeamRank } from "@/lib/types";

export async function fetchTeamRankings(): Promise<TeamRank[]> {
  return apiFetch<TeamRank[]>("/teams");
}

export async function fetchTeamRoster(teamId: string): Promise<RosterPlayer[]> {
  return apiFetch<RosterPlayer[]>(`/teams/${teamId}/roster`);
}

/** Power rankings (rang Elo et évolution sur 7 jours, null sans photo assez ancienne). */
export interface PowerRankingEntry {
  teamId: string;
  abbreviation: string;
  rank: number;
  previousRank: number | null;
  elo: number;
  eloChange: number | null;
}

export async function fetchPowerRankings(): Promise<PowerRankingEntry[]> {
  return apiFetch<PowerRankingEntry[]>("/teams/power-rankings");
}
