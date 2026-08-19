import { apiFetch } from "@/lib/api/http";
import { Player } from "@/lib/types";

export async function fetchPlayers(params?: { teamId?: string; search?: string }): Promise<Player[]> {
  const query = new URLSearchParams();
  if (params?.teamId) query.set("teamId", params.teamId);
  if (params?.search) query.set("search", params.search);
  const qs = query.toString();
  return apiFetch<Player[]>(`/players${qs ? `?${qs}` : ""}`);
}
