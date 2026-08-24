import { apiFetch } from "@/lib/api/http";
import { AdminBet, AdminStatus, AdminUser, Match } from "@/lib/types";

export async function fetchAdminStatus(): Promise<AdminStatus> {
  return apiFetch<AdminStatus>("/console/status");
}

export async function fetchAdminUsers(search?: string): Promise<AdminUser[]> {
  const params = search ? `?search=${encodeURIComponent(search)}` : "";
  return apiFetch<AdminUser[]>(`/console/users${params}`);
}

export async function toggleAdminStatus(userId: string): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/console/users/${userId}/toggle-admin`, { method: "POST" });
}

export async function deleteAdminUser(userId: string): Promise<void> {
  return apiFetch<void>(`/console/users/${userId}/delete`, { method: "POST" });
}

export async function fetchAdminMatches(search?: string, status?: string): Promise<Match[]> {
  const params = new URLSearchParams();
  if (search) params.set("search", search);
  if (status) params.set("status", status);
  const qs = params.toString();
  return apiFetch<Match[]>(`/console/matches${qs ? `?${qs}` : ""}`);
}

export async function updateAdminMatch(
  matchId: string,
  body: { status?: string; homeScore?: number; awayScore?: number }
): Promise<Match> {
  return apiFetch<Match>(`/console/matches/${matchId}`, {
    method: "PATCH",
    body: JSON.stringify(body),
  });
}

export async function fetchPendingBets(): Promise<AdminBet[]> {
  return apiFetch<AdminBet[]>("/console/bets/pending");
}

export async function syncTeams(): Promise<{ teamsSynced: number }> {
  return apiFetch("/console/sync-teams", { method: "POST" });
}

export async function syncGames(
  daysAhead: number,
  startDate?: string
): Promise<{ gamesSynced: number }> {
  const params = new URLSearchParams({ daysAhead: String(daysAhead) });
  if (startDate) params.set("startDate", startDate);
  return apiFetch(`/console/sync-games?${params.toString()}`, { method: "POST" });
}

export async function resolveBets(): Promise<{ resolved: number }> {
  return apiFetch("/console/resolve-bets", { method: "POST" });
}
