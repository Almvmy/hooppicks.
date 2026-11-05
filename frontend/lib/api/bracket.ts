import { apiFetch } from "@/lib/api/http";
import type { AvatarColorway, AvatarIcon, AvatarPosition } from "@/lib/types";

export interface BracketGame {
  index: number;
  round: number;
  points: number;
  teamA: string | null;
  teamB: string | null;
  winner: string | null;
}

export interface BracketRankingEntry {
  rank: number;
  username: string;
  avatarNumber: number;
  avatarPosition: AvatarPosition;
  avatarColorway: AvatarColorway;
  avatarIcon: AvatarIcon;
  points: number;
  champion: string;
  me: boolean;
}

export interface Bracket {
  id: string;
  label: string;
  locksAt: string;
  locked: boolean;
  teams: number;
  games: BracketGame[];
  /** Null : pas encore rempli. */
  myPicks: (string | null)[] | null;
  myPoints: number | null;
  players: number;
  /** Null avant le verrou : les choix des autres restent cachés. */
  ranking: BracketRankingEntry[] | null;
  champions: { team: string; players: number }[] | null;
}

const post = <T,>(path: string, body: unknown) => apiFetch<T>(path, { method: "POST", body: JSON.stringify(body) });

/** Null : pas encore de tableau (le serveur répond 204 avant la fin des poules). */
export const fetchBracket = async () => (await apiFetch<Bracket | undefined>("/bracket")) ?? null;
export const saveBracketPicks = (picks: string[]) => post<Bracket>("/bracket/picks", { picks });
export const publishBracket = (bracket: { id: string; label: string; teams: string[]; locksAt: string }) => post<unknown>("/console/bracket", bracket);
export const setBracketWinner = (game: number, team: string | null) => post<unknown>("/console/bracket/winner", { game, team });

/** Les deux équipes d'un match d'après les choix des tours d'avant (même règle que BracketTree). */
export function pairOf(games: BracketGame[], teams: number, picks: (string | null)[], game: number): [string | null, string | null] {
  const first = teams / 2;
  if (game < first) return [games[game].teamA, games[game].teamB];
  const child = (game - first) * 2;
  return [picks[child] ?? null, picks[child + 1] ?? null];
}

/** Choisir un vainqueur : les tours suivants qui reposaient sur l'ancien choix se vident. */
export function withPick(games: BracketGame[], teams: number, picks: (string | null)[], game: number, team: string): (string | null)[] {
  const next = [...picks];
  next[game] = team;
  for (let g = game + 1; g < next.length; g++) {
    const pair = pairOf(games, teams, next, g);
    if (next[g] && !pair.includes(next[g])) next[g] = null;
  }
  return next;
}

/** Quarts de finale, Demi-finales, Finale, en partant du dernier tour. */
export function roundLabel(games: BracketGame[], round: number): string {
  const last = Math.max(...games.map((g) => g.round));
  const fromEnd = last - round;
  return ["Finale", "Demi-finales", "Quarts de finale", "Huitièmes de finale"][fromEnd] ?? `Tour ${round + 1}`;
}
