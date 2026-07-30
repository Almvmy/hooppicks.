import { apiFetch } from "@/lib/api/http";
import { Match } from "@/lib/types";

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