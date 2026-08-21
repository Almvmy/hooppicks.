import { apiFetch } from "@/lib/api/http";
import { League, LeagueActivity, LeagueMember, LeaguePreview, LeaderboardEntry } from "@/lib/types";

export async function fetchMyLeagues(): Promise<League[]> {
  return apiFetch<League[]>("/leagues");
}

export async function previewLeague(inviteCode: string): Promise<LeaguePreview> {
  return apiFetch<LeaguePreview>(`/leagues/preview/${inviteCode}`);
}

export async function fetchLeagueMembers(id: string): Promise<LeagueMember[]> {
  return apiFetch<LeagueMember[]>(`/leagues/${id}/members`);
}

export async function fetchLeagueActivity(id: string): Promise<LeagueActivity[]> {
  return apiFetch<LeagueActivity[]>(`/leagues/${id}/activity`);
}

export async function createLeague(name: string): Promise<League> {
  return apiFetch<League>("/leagues", {
    method: "POST",
    body: JSON.stringify({ name }),
  });
}

export async function joinLeague(inviteCode: string): Promise<League> {
  return apiFetch<League>("/leagues/join", {
    method: "POST",
    body: JSON.stringify({ inviteCode }),
  });
}

export async function fetchLeagueLeaderboard(id: string): Promise<LeaderboardEntry[]> {
  return apiFetch<LeaderboardEntry[]>(`/leagues/${id}/leaderboard`);
}

export async function leaveLeague(id: string): Promise<void> {
  await apiFetch<void>(`/leagues/${id}/leave`, { method: "POST" });
}
