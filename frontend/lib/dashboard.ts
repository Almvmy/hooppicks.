import {
  LeaderboardEntry,
  Match,
  PlacedBet,
} from "@/lib/types";
import { nextBankrollReset } from "@/lib/utils";

/**
 * Série de victoires en cours : on part du pari le plus récent (les paris
 * sont renvoyés triés par date décroissante) et on compte les "won"
 * consécutifs. Les paris "pending" et "void" n'interrompent pas la série,
 * seule une défaite le fait.
 */
export function computeWinStreak(bets: PlacedBet[] | undefined): number {
  if (!bets) return 0;
  let streak = 0;
  for (const bet of bets) {
    if (bet.status === "pending" || bet.status === "void") continue;
    if (bet.status === "won") {
      streak++;
      continue;
    }
    break; // "lost"
  }
  return streak;
}

export function pendingBetsSummary(bets: PlacedBet[] | undefined) {
  const pending = bets?.filter((b) => b.status === "pending") ?? [];
  return {
    count: pending.length,
    stake: pending.reduce((sum, b) => sum + b.stake, 0),
    potential: pending.reduce((sum, b) => sum + b.potentialPayout, 0),
  };
}

/**
 * Reconstitue une série chronologique du solde à partir des transactions
 * (triées date décroissante côté API) et du solde actuel, pour tracer une
 * mini-courbe de tendance.
 */
/**
 * La semaine de jeu en cours (depuis lundi 12h GMT) : bénéfice net cumulé
 * ticket après ticket, dans l'ordre où ils se sont réglés, mises et gains.
 * Calculé sur les paris et non sur le solde, qui remonte à 1 000 chaque
 * lundi et rendait la courbe « solde des 7 derniers jours » illisible.
 */
export function weekSummary(bets: PlacedBet[] | undefined, now: Date = new Date()) {
  const weekStart = nextBankrollReset(now).getTime() - 7 * 24 * 60 * 60 * 1000;
  const thisWeek = (bets ?? []).filter((b) => new Date(b.placedAt).getTime() >= weekStart);
  const settled = thisWeek
    .filter((b) => b.status === "won" || b.status === "lost")
    .sort((a, b) => new Date(a.resolvedAt ?? a.placedAt).getTime() - new Date(b.resolvedAt ?? b.placedAt).getTime());

  const series = [0];
  for (const b of settled) {
    series.push(series[series.length - 1] + (b.status === "won" ? b.potentialPayout - b.stake : -b.stake));
  }
  if (series.length === 1) series.push(0);

  // Mêmes tickets pour les trois chiffres que pour la courbe (réglés
  // seulement) : rapporté − misé = bénéfice affiché. Avant, « misé » comptait
  // aussi les tickets en attente et remboursés, et ne collait plus au total.
  return {
    series,
    net: series[series.length - 1],
    staked: settled.reduce((sum, b) => sum + b.stake, 0),
    won: settled.filter((b) => b.status === "won").reduce((sum, b) => sum + b.potentialPayout, 0),
    pending: thisWeek.filter((b) => b.status === "pending").reduce((sum, b) => sum + b.stake, 0),
  };
}

export interface DashboardSlate {
  live: Match[];
  spotlight?: Match;
  upcoming: Match[];
}

/**
 * Sélectionne le match "à la une" (un direct en priorité, sinon le
 * prochain coup d'envoi) et une petite liste de matchs à venir pour
 * remplir le dashboard sans dupliquer la page Matchs.
 */
export function buildDashboardSlate(matches: Match[] | undefined): DashboardSlate {
  if (!matches) return { live: [], upcoming: [] };

  const now = Date.now();
  const live = matches
    .filter((m) => m.status === "live")
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());

  const scheduled = matches
    .filter((m) => m.status === "scheduled" && new Date(m.date).getTime() >= now)
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());

  const spotlight = live[0] ?? scheduled[0];
  const upcoming = scheduled.filter((m) => m.id !== spotlight?.id).slice(0, 3);

  return { live, spotlight, upcoming };
}

export function findLeaderboardEntry(
  leaderboard: LeaderboardEntry[] | undefined,
  username: string | undefined
): LeaderboardEntry | undefined {
  if (!leaderboard || !username) return undefined;
  return leaderboard.find((e) => e.username === username);
}

export function greetingForNow(): string {
  const hour = new Date().getHours();
  if (hour < 6) return "Bonne nuit";
  if (hour < 18) return "Bonjour";
  return "Bonsoir";
}
