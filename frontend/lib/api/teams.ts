import { apiFetch } from "@/lib/api/http";
import { TeamRank } from "@/lib/types";

export async function fetchTeamRankings(): Promise<TeamRank[]> {
  return apiFetch<TeamRank[]>("/teams");
}
