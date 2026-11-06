import { apiFetch } from "@/lib/api/http";
import type { NightFeed } from "@/lib/api/night-thread";

export interface PastDebate {
  day: string;
  question: string;
  optionA: string;
  optionB: string;
  votesA: number;
  votesB: number;
}

export interface Debate {
  day: string;
  question: string;
  optionA: string;
  optionB: string;
  open: boolean;
  myVote: "A" | "B" | null;
  /** Null tant que le joueur n'a pas voté : on se fait d'abord son avis. */
  votesA: number | null;
  votesB: number | null;
  total: number;
  comments: number;
  history: PastDebate[];
  /** Photo ou logo de chaque camp, quand c'est un joueur ou une équipe connus. */
  imageA: string | null;
  imageB: string | null;
  /** Le joueur ou l'équipe dont parle un débat d'actualité. */
  imageUrl: string | null;
}

export interface PlannedDebate {
  day: string;
  question: string;
  optionA: string;
  optionB: string;
  planned: boolean;
  votes: number;
}

const post = <T,>(path: string, body?: unknown) =>
  apiFetch<T>(path, { method: "POST", body: body === undefined ? undefined : JSON.stringify(body) });

export const fetchTodayDebate = () => apiFetch<Debate>("/debates/today");
export const fetchDebate = (day: string) => apiFetch<Debate>(`/debates/${day}`);
export const voteDebate = (side: "A" | "B") => post<Debate>("/debates/today/vote", { side });
export const fetchDebateComments = (day: string) => apiFetch<NightFeed>(`/debates/${day}/comments`);
export const postDebateComment = (day: string, text: string, replyToId?: string) => post<NightFeed>(`/debates/${day}/comments`, { text, replyToId: replyToId ?? null });

export const fetchDebatePlanning = () => apiFetch<PlannedDebate[]>("/console/debates");
export const planDebate = (debate: { day: string; question: string; optionA: string; optionB: string }) =>
  post<PlannedDebate[]>("/console/debates", debate);
export const resetDebate = (day: string) => post<PlannedDebate[]>(`/console/debates/${day}/reset`);

/** Part d'un camp, en pourcentage entier. */
export function debateShare(votes: number, other: number): number {
  const total = votes + other;
  return total === 0 ? 0 : Math.round((votes * 100) / total);
}

/** « 5 novembre » à partir d'un jour de débat (« 2026-11-05 »), en GMT. */
export function debateDayLabel(day: string): string {
  return new Date(`${day}T12:00:00Z`).toLocaleDateString("fr-FR", { day: "numeric", month: "long", timeZone: "UTC" });
}
