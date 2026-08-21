import { apiFetch } from "@/lib/api/http";
import { AdminStatus } from "@/lib/types";

export async function fetchAdminStatus(): Promise<AdminStatus> {
  return apiFetch<AdminStatus>("/console/status");
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
