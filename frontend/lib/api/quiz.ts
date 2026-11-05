import { apiFetch } from "@/lib/api/http";
import type { AvatarColorway, AvatarIcon, AvatarPosition } from "@/lib/types";

export interface QuizQuestion {
  index: number;
  total: number;
  text: string;
  imageUrl: string | null;
  choices: string[];
  secondsLeft: number;
}

export interface QuizStanding {
  /** 0 : pas encore classé cette semaine. */
  rank: number;
  players: number;
  baskets: number;
  days: number;
  level: string;
}

export interface QuizState {
  day: string;
  status: "ready" | "playing" | "done";
  /** Un caractère par question jouée : 1 panier, 0 raté. */
  shots: string;
  question: QuizQuestion | null;
  review: { text: string; answer: string; hit: boolean }[] | null;
  standing: QuizStanding;
}

export interface QuizAnswerResult {
  hit: boolean;
  correct: number;
  state: QuizState;
}

export interface QuizBoardEntry {
  rank: number;
  username: string;
  avatarNumber: number;
  avatarPosition: AvatarPosition;
  avatarColorway: AvatarColorway;
  avatarIcon: AvatarIcon;
  baskets: number;
  days: number;
  seconds: number;
  level: string;
  me: boolean;
}

export interface QuizBoard {
  week: string;
  entries: QuizBoardEntry[];
  me: QuizBoardEntry | null;
  lastChampion: { username: string; baskets: number; week: string } | null;
}

export const fetchQuiz = () => apiFetch<QuizState>("/quiz");
export const startQuiz = () => apiFetch<QuizState>("/quiz/start", { method: "POST" });
export const answerQuiz = (index: number, choice: number | null) =>
  apiFetch<QuizAnswerResult>("/quiz/answer", { method: "POST", body: JSON.stringify({ index, choice }) });
export const fetchQuizBoard = () => apiFetch<QuizBoard>("/quiz/board");

/** 🏀🏀❌🏀🏀 : à partager sans dévoiler les réponses. */
export function shotsLine(shots: string): string {
  return [...shots].map((s) => (s === "1" ? "🏀" : "❌")).join("");
}

export function shotsCount(shots: string): number {
  return [...shots].filter((s) => s === "1").length;
}
