import { apiFetch } from "@/lib/api/http";
import { Match, PlayerBoxScore, PlayerProp } from "@/lib/types";

const DAY_MS = 24 * 60 * 60 * 1000;
/** Au-delà de la semaine de jeu, aucun match n'est ouvert aux paris (synchro jusqu'à J+7). */
const DAYS_AHEAD = 8;
export const DEFAULT_PAST_DAYS = 14;

function utcDay(offsetDays: number): string {
  return new Date(Date.now() + offsetDays * DAY_MS).toISOString().slice(0, 10);
}

/**
 * Matchs autour d'aujourd'hui, pas toute la saison : la réponse grossissait
 * chaque semaine. La page Matchs remonte plus loin à la demande (pastDays).
 */
export async function fetchMatches(pastDays: number = DEFAULT_PAST_DAYS): Promise<Match[]> {
  return apiFetch<Match[]>(`/matches?from=${utcDay(-pastDays)}&to=${utcDay(DAYS_AHEAD)}`);
}

export async function fetchTeamMatches(teamId: string): Promise<Match[]> {
  return apiFetch<Match[]>(`/matches?teamId=${encodeURIComponent(teamId)}`);
}

export async function fetchMatchesByIds(ids: string[]): Promise<Match[]> {
  if (ids.length === 0) return [];
  return apiFetch<Match[]>(`/matches?ids=${ids.map(encodeURIComponent).join(",")}`);
}

export async function fetchMatchById(id: string): Promise<Match | undefined> {
  try {
    return await apiFetch<Match>(`/matches/${id}`);
  } catch {
    return undefined;
  }
}

export async function fetchMatchBoxScore(id: string): Promise<PlayerBoxScore[]> {
  return apiFetch<PlayerBoxScore[]>(`/matches/${id}/boxscore`);
}

export async function fetchPlayerProps(matchId: string): Promise<PlayerProp[]> {
  return apiFetch<PlayerProp[]>(`/matches/${matchId}/props`);
}

/** Paris joueurs de plusieurs matchs (une soirée NBA, 20 matchs au plus). */
export async function fetchPlayerPropsOf(matchIds: string[]): Promise<PlayerProp[]> {
  if (matchIds.length === 0) return [];
  return apiFetch<PlayerProp[]>(`/props?ids=${matchIds.map(encodeURIComponent).join(",")}`);
}
