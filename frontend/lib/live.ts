import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api/http";
import type { Match, PlayerBoxScore } from "@/lib/types";

/**
 * Le direct (LiveMatchService, données ESPN) : affichage seulement. Le statut
 * et le score « officiels » restent ceux du match (synchro balldontlie, qui
 * règle les paris). Si ESPN ne répond plus, le serveur ne renvoie rien et
 * tout ce qui suit retombe sur le match tel quel.
 */
export interface LiveStatus {
  matchId: string;
  state: "pre" | "in" | "post";
  period: number;
  clock: string;
  detail: string;
  homeScore: number | null;
  awayScore: number | null;
}

export interface LiveSituation {
  possession: string | null;
  homeFouls: number | null;
  awayFouls: number | null;
  /** « SINGLE » / « DOUBLE » : l'adversaire tire des lancers sur chaque faute. */
  homeBonus: string | null;
  awayBonus: string | null;
}

/** Un tir sur le demi-terrain, en pieds : panier vers (25, 0), ligne de fond vers y = −5. */
export interface LiveShot {
  x: number;
  y: number;
  made: boolean;
  points: number;
  teamAbbreviation: string | null;
  playerName: string | null;
}

export type KeyPlayKind =
  | "three"
  | "dunk"
  | "layup"
  | "jumper"
  | "hook"
  | "alley_oop"
  | "free_throw"
  | "end_period"
  | "end_game";

export interface KeyPlay {
  period: number;
  clock: string;
  kind: KeyPlayKind;
  points: number;
  teamAbbreviation: string | null;
  playerName: string | null;
  awayScore: number;
  homeScore: number;
}

export interface LiveMatch {
  status: LiveStatus;
  homeLinescores: number[];
  awayLinescores: number[];
  teamStats: { label: string; home: string; away: string }[];
  players: PlayerBoxScore[];
  /** Pendant le jeu seulement. */
  situation: LiveSituation | null;
  /** Probabilité de victoire de l'équipe à domicile au fil du match. */
  winProbability: { elapsedSeconds: number; homeWinPct: number }[];
  shots: LiveShot[];
  keyPlays: KeyPlay[];
}

const REFRESH = 30 * 1000;

/** Statut en direct d'un match en cours (une seule requête partagée par toute l'app). */
export function useLiveStatus(match: Match | undefined): LiveStatus | undefined {
  const { data } = useQuery({
    queryKey: ["live-board"],
    queryFn: () => apiFetch<LiveStatus[]>("/matches/live"),
    enabled: match?.status === "live",
    refetchInterval: REFRESH,
    staleTime: REFRESH,
  });
  if (match?.status !== "live") return undefined;
  return data?.find((s) => s.matchId === match.id);
}

/**
 * Score par quart-temps, stats, feuille de match : en direct pendant le match,
 * et toujours consultables après (ESPN garde le résumé d'un match terminé).
 * null : pas de données ESPN pour ce match, ou ESPN injoignable.
 */
export function useLiveMatch(match: Match | undefined) {
  const live = match?.status === "live";
  return useQuery({
    queryKey: ["live", match?.id],
    queryFn: async () => (await apiFetch<LiveMatch | undefined>(`/matches/${match!.id}/live`)) ?? null,
    enabled: live || match?.status === "finished",
    refetchInterval: live ? REFRESH : false,
    staleTime: live ? REFRESH : 10 * 60 * 1000,
  });
}

/**
 * Le match avec le score du direct, plus frais que celui de la synchro (5 min).
 * Le statut ne change jamais : c'est la synchro qui dit qu'un match est fini.
 */
export function withLiveScore(match: Match, live: LiveStatus | undefined): Match {
  if (!live || live.homeScore == null || live.awayScore == null) return match;
  return { ...match, homeScore: live.homeScore, awayScore: live.awayScore };
}

const ordinal = (n: number) => (n === 1 ? "1er" : `${n}e`);

/** « 3e QT · 5:42 », « Mi-temps », « Fin du 1er QT », « Prolongation · 2:10 ». */
export function liveClockLabel(live: LiveStatus): string {
  if (live.state === "post") return "Terminé";
  if (/halftime/i.test(live.detail)) return "Mi-temps";
  const period =
    live.period <= 4 ? `${ordinal(live.period)} QT` : live.period === 5 ? "Prolongation" : `${live.period - 4}e prol.`;
  const ended = live.clock === "0.0" || live.clock === "0:00";
  if (ended) return live.period <= 4 ? `Fin du ${period}` : `Fin ${period === "Prolongation" ? "de la prolongation" : `de la ${period}`}`;
  return `${period} · ${live.clock}`;
}

/** Libellé court d'une colonne de quart-temps : « 1 », « 2 »… puis « P1 », « P2 ». */
export function periodColumnLabel(index: number): string {
  return index < 4 ? String(index + 1) : `P${index - 3}`;
}
