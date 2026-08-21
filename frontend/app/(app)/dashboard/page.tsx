"use client";

import { useQuery } from "@tanstack/react-query";
import { Wallet, Target, Trophy, Ticket } from "lucide-react";

import { fetchProfile } from "@/lib/api/auth";
import { fetchWallet, fetchWalletTransactions } from "@/lib/api/wallet";
import { fetchBets } from "@/lib/api/bets";
import { fetchMatches } from "@/lib/api/matches";
import { fetchLeaderboard } from "@/lib/api/leaderboard";

import {
  buildDashboardSlate,
  buildWalletSeries,
  computeWinStreak,
  findLeaderboardEntry,
  pendingBetsSummary,
  weeklyStakedAndWon,
  weeklyWalletDelta,
} from "@/lib/dashboard";

import { DashboardHero } from "@/components/dashboard/dashboard-hero";
import { DashboardStats } from "@/components/dashboard/dashboard-stats";
import { PickOfDay } from "@/components/dashboard/pick-of-day";
import { UpcomingMatches } from "@/components/dashboard/upcoming-matches";
import { RecentActivity } from "@/components/dashboard/recent-activity";
import { WalletTrend } from "@/components/dashboard/wallet-trend";
import { LeaderboardPreview } from "@/components/dashboard/leaderboard-preview";
import { FunFactCard } from "@/components/dashboard/fun-fact-card";

export default function DashboardPage() {
  const profileQuery = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const walletQuery = useQuery({ queryKey: ["wallet"], queryFn: fetchWallet });
  const transactionsQuery = useQuery({
    queryKey: ["wallet-transactions"],
    queryFn: fetchWalletTransactions,
  });
  const betsQuery = useQuery({ queryKey: ["bets"], queryFn: fetchBets });
  const matchesQuery = useQuery({
    queryKey: ["matches"],
    queryFn: fetchMatches,
    refetchInterval: 60 * 1000,
  });
  const leaderboardQuery = useQuery({
    queryKey: ["leaderboard"],
    queryFn: fetchLeaderboard,
    staleTime: 2 * 60 * 1000,
  });

  const streak = computeWinStreak(betsQuery.data);
  const pending = pendingBetsSummary(betsQuery.data);
  const slate = buildDashboardSlate(matchesQuery.data);
  const walletSeries = buildWalletSeries(transactionsQuery.data, walletQuery.data?.balance);
  const weeklyDelta = weeklyWalletDelta(transactionsQuery.data);
  const { staked, won } = weeklyStakedAndWon(transactionsQuery.data);
  const ownEntry = findLeaderboardEntry(leaderboardQuery.data, profileQuery.data?.username);

  const statsLoading = profileQuery.isLoading || walletQuery.isLoading || leaderboardQuery.isLoading || betsQuery.isLoading;

  return (
    <div className="flex flex-col gap-6">
      <DashboardHero
        username={profileQuery.data?.username}
        isLoading={profileQuery.isLoading}
        streak={streak}
        pendingCount={pending.count}
      />

      <DashboardStats
        items={[
          {
            label: "Solde actuel",
            value: walletQuery.data ? `${walletQuery.data.balance.toLocaleString("fr-FR")} pts` : undefined,
            icon: Wallet,
            isLoading: statsLoading,
          },
          {
            label: "Taux de réussite",
            value: profileQuery.data ? `${profileQuery.data.winRate}%` : undefined,
            hint: profileQuery.data ? `sur ${profileQuery.data.totalBets} paris` : undefined,
            icon: Target,
            isLoading: statsLoading,
          },
          {
            label: "Rang classement",
            value: ownEntry ? `#${ownEntry.rank}` : "—",
            hint: leaderboardQuery.data ? `sur ${leaderboardQuery.data.length} joueurs` : undefined,
            icon: Trophy,
            isLoading: statsLoading,
          },
          {
            label: "Paris en cours",
            value: `${pending.count}`,
            hint: pending.count > 0 ? `${pending.stake.toLocaleString("fr-FR")} pts engagés` : "aucun ticket ouvert",
            icon: Ticket,
            isLoading: statsLoading,
          },
        ]}
      />

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="flex flex-col gap-6 lg:col-span-2">
          <PickOfDay match={slate.spotlight} />
          <UpcomingMatches matches={slate.upcoming} isLoading={matchesQuery.isLoading} />
          <RecentActivity bets={(betsQuery.data ?? []).slice(0, 3)} isLoading={betsQuery.isLoading} />
        </div>

        <div className="flex flex-col gap-6">
          <WalletTrend
            series={walletSeries}
            weeklyDelta={weeklyDelta}
            staked={staked}
            won={won}
            isLoading={walletQuery.isLoading || transactionsQuery.isLoading}
          />
          <LeaderboardPreview
            entries={leaderboardQuery.data ?? []}
            currentUsername={profileQuery.data?.username}
            isLoading={leaderboardQuery.isLoading}
          />
          <FunFactCard />
        </div>
      </div>
    </div>
  );
}
