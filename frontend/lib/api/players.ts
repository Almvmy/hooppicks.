import { apiFetch } from "@/lib/api/http";
import { PlayerLeaders, PlayerRecentGame, RosterPlayer } from "@/lib/types";

export async function fetchPlayers(params?: { teamId?: string; search?: string }): Promise<RosterPlayer[]> {
  const query = new URLSearchParams();
  if (params?.teamId) query.set("teamId", params.teamId);
  if (params?.search) query.set("search", params.search);
  const qs = query.toString();
  return apiFetch<RosterPlayer[]>(`/players${qs ? `?${qs}` : ""}`);
}

export async function fetchPlayerLeaders(): Promise<PlayerLeaders> {
  return apiFetch<PlayerLeaders>("/players/leaders");
}

/** Joueurs avec un statut de blessure (Out, Day-To-Day…), triés par équipe. */
export async function fetchPlayerInjuries(): Promise<RosterPlayer[]> {
  return apiFetch<RosterPlayer[]>("/players/injuries");
}

export async function fetchPlayerRecentGames(id: string): Promise<PlayerRecentGame[]> {
  return apiFetch<PlayerRecentGame[]>(`/players/${id}/recent-games`);
}
