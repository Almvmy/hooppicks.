import { apiFetch } from "@/lib/api/http";

export interface SeasonQuestion {
  key: "champion" | "east_finalist" | "west_finalist" | "best_record";
  label: string;
  points: number;
}

export interface SeasonPicksOverview {
  season: string;
  /** Premier match de saison régulière : null tant qu'il n'est pas connu. */
  deadline: string | null;
  locked: boolean;
  questions: SeasonQuestion[];
  myPicks: Record<string, string>;
  results: Record<string, string>;
  /** Répartition des choix, visible une fois verrouillé seulement. */
  community: Record<string, Record<string, number>>;
  leaderboard: { username: string; points: number; correct: number }[];
}

export async function fetchSeasonPicks(): Promise<SeasonPicksOverview> {
  return apiFetch<SeasonPicksOverview>("/season-picks");
}

export async function saveSeasonPick(question: string, team: string): Promise<void> {
  return apiFetch<void>("/season-picks", { method: "PUT", body: JSON.stringify({ question, team }) });
}

export async function setSeasonResult(question: string, team: string): Promise<void> {
  return apiFetch<void>("/console/season-results", { method: "POST", body: JSON.stringify({ question, team }) });
}
