import { apiFetch } from "@/lib/api/http";
import { AdminAuditEntry, AdminBet, AdminMatch, AdminOverview, AdminStatus, AdminUser, Match } from "@/lib/types";

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

export async function fetchAdminMatches(search?: string, status?: string): Promise<AdminMatch[]> {
  const params = new URLSearchParams();
  if (search) params.set("search", search);
  if (status) params.set("status", status);
  const qs = params.toString();
  return apiFetch<AdminMatch[]>(`/console/matches${qs ? `?${qs}` : ""}`);
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

export async function syncRosters(): Promise<{ synced: boolean }> {
  return apiFetch("/console/sync-rosters", { method: "POST" });
}

export async function syncStandings(): Promise<{ synced: boolean }> {
  return apiFetch("/console/sync-standings", { method: "POST" });
}

export async function syncPlayerStatsBatch(): Promise<{ synced: boolean }> {
  return apiFetch("/console/sync-player-stats-batch", { method: "POST" });
}

export async function fetchAdminOverview(): Promise<AdminOverview> {
  return apiFetch<AdminOverview>("/console/overview");
}

export async function fetchAdminAudit(limit = 100): Promise<AdminAuditEntry[]> {
  return apiFetch<AdminAuditEntry[]>(`/console/audit?limit=${limit}`);
}

export async function unlockAdminMatch(matchId: string): Promise<Match> {
  return apiFetch<Match>(`/console/matches/${matchId}/unlock`, { method: "POST" });
}

export async function voidMatchBets(matchId: string, reason: string): Promise<{ voided: number }> {
  return apiFetch(`/console/matches/${matchId}/void-bets`, { method: "POST", body: JSON.stringify({ reason }) });
}

export async function voidBet(betId: string, reason: string): Promise<{ voided: boolean }> {
  return apiFetch(`/console/bets/${betId}/void`, { method: "POST", body: JSON.stringify({ reason }) });
}

export async function adjustWallet(userId: string, amount: number, reason: string): Promise<{ balance: number }> {
  return apiFetch(`/console/users/${userId}/adjust-wallet`, {
    method: "POST",
    body: JSON.stringify({ amount, reason }),
  });
}

export async function sendAnnouncement(message: string, push: boolean): Promise<{ recipients: number }> {
  return apiFetch("/console/announcements", { method: "POST", body: JSON.stringify({ message, push }) });
}
