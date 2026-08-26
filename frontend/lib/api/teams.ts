import { apiFetch } from "@/lib/api/http";
import { RosterPlayer, TeamRank } from "@/lib/types";

export async function fetchTeamRankings(): Promise<TeamRank[]> {
  return apiFetch<TeamRank[]>("/teams");
}

export async function fetchTeamRoster(teamId: string): Promise<RosterPlayer[]> {
  return apiFetch<RosterPlayer[]>(`/teams/${teamId}/roster`);
}
