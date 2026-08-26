import { apiFetch } from "@/lib/api/http";
import { Match, PlayerBoxScore } from "@/lib/types";

export async function fetchMatches(): Promise<Match[]> {
  return apiFetch<Match[]>("/matches");
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