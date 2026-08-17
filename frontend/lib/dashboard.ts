import {
  LeaderboardEntry,
  Match,
  PlacedBet,
  WalletTransaction,
} from "@/lib/types";

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
export function buildWalletSeries(
  transactions: WalletTransaction[] | undefined,
  currentBalance: number | undefined
): number[] {
  if (currentBalance === undefined) return [];
  if (!transactions || transactions.length === 0) {
    return [currentBalance, currentBalance];
  }

  const chronological = [...transactions].reverse(); // plus ancien -> plus récent
  const totalDelta = transactions.reduce((sum, t) => sum + t.amount, 0);
  let running = currentBalance - totalDelta;

  const points = [running];
  for (const t of chronological) {
    running += t.amount;
    points.push(running);
  }
  return points;
}

export function weeklyWalletDelta(transactions: WalletTransaction[] | undefined): number {
  if (!transactions) return 0;
  const weekAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;
  return transactions
    .filter((t) => new Date(t.date).getTime() >= weekAgo)
    .reduce((sum, t) => sum + t.amount, 0);
}

export function weeklyStakedAndWon(transactions: WalletTransaction[] | undefined) {
  const weekAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;
  const recent = (transactions ?? []).filter(
    (t) => new Date(t.date).getTime() >= weekAgo
  );
  const staked = recent
    .filter((t) => t.type === "bet_placed")
    .reduce((sum, t) => sum + Math.abs(t.amount), 0);
  const won = recent
    .filter((t) => t.type === "bet_win")
    .reduce((sum, t) => sum + t.amount, 0);
  return { staked, won };
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
