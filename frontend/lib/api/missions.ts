import { apiFetch } from "@/lib/api/http";

export interface WeeklyMissions {
  week: string;
  missions: { key: string; label: string; progress: number; target: number; done: boolean }[];
  /** Missions réussies depuis le début. */
  completed: number;
  /** null : toutes les récompenses sont débloquées. */
  nextRewardAt: number | null;
  nextRewardLabel: string | null;
}

export async function fetchMissions(): Promise<WeeklyMissions> {
  return apiFetch<WeeklyMissions>("/missions");
}
