"use client";

import { useQuery } from "@tanstack/react-query";
import { Wallet, Target, Trophy, Ticket } from "lucide-react";
import { WEEKLY_BANKROLL, formatBankrollReset, formatSignedPoints } from "@/lib/utils";

import { fetchProfile } from "@/lib/api/auth";
import { fetchWallet } from "@/lib/api/wallet";
import { fetchBets } from "@/lib/api/bets";
import { fetchMatches } from "@/lib/api/matches";
import { fetchLeaderboard, fetchMyStanding } from "@/lib/api/leaderboard";
import { fetchNews } from "@/lib/api/news";
import { fetchMyLeagues } from "@/lib/api/leagues";

import { buildDashboardSlate, computeWinStreak, pendingBetsSummary, weekSummary } from "@/lib/dashboard";

import { DashboardHero } from "@/components/dashboard/dashboard-hero";
import { DashboardStats } from "@/components/dashboard/dashboard-stats";
import { PickOfDay } from "@/components/dashboard/pick-of-day";
import { UpcomingMatches } from "@/components/dashboard/upcoming-matches";
import { RecentActivity } from "@/components/dashboard/recent-activity";
import { WalletTrend } from "@/components/dashboard/wallet-trend";
import { LeaderboardPreview } from "@/components/dashboard/leaderboard-preview";
import { LeaguesPreview } from "@/components/dashboard/leagues-preview";
import { NewsPreview } from "@/components/dashboard/news-preview";
import { FavoriteTeamCard } from "@/components/dashboard/favorite-team-card";
import { FunFactCard } from "@/components/dashboard/fun-fact-card";
import { SeasonPicksNudge } from "@/components/dashboard/season-picks-nudge";
import { BoostCard } from "@/components/dashboard/boost-card";
import { SuggestedParlays } from "@/components/dashboard/suggested-parlays";
import { useBoost } from "@/lib/boost";
import { CountUp } from "@/components/motion/count-up";

export default function DashboardPage() {
  const profileQuery = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const walletQuery = useQuery({ queryKey: ["wallet"], queryFn: fetchWallet });
  const standingQuery = useQuery({ queryKey: ["leaderboard", "me"], queryFn: fetchMyStanding, refetchInterval: 60_000 });
  const betsQuery = useQuery({ queryKey: ["bets"], queryFn: fetchBets });
  const matchesQuery = useQuery({
    queryKey: ["matches"],
    queryFn: fetchMatches,
    refetchInterval: 60 * 1000,
  });
  const leaderboardQuery = useQuery({
    queryKey: ["leaderboard"],
    queryFn: () => fetchLeaderboard(),
    staleTime: 2 * 60 * 1000,
  });
  const newsQuery = useQuery({
    queryKey: ["news"],
    queryFn: fetchNews,
    staleTime: 10 * 60 * 1000,
  });
  const leaguesQuery = useQuery({
    queryKey: ["leagues"],
    queryFn: fetchMyLeagues,
    staleTime: 5 * 60 * 1000,
  });

  const streak = computeWinStreak(betsQuery.data);
  const pending = pendingBetsSummary(betsQuery.data);
  const boost = useBoost();
  // Le match boosté a sa propre carte : pas une seconde fois « à la une »
  // (sauf en direct, où la une sert à suivre le score).
  const slate = buildDashboardSlate(
    matchesQuery.data?.filter((m) => m.id !== boost?.matchId || m.status === "live")
  );
  const week = weekSummary(betsQuery.data);
  const standing = standingQuery.data;

  const statsLoading = profileQuery.isLoading || walletQuery.isLoading || standingQuery.isLoading || betsQuery.isLoading;

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
            label: "Solde de la semaine",
            value: walletQuery.data ? (
              <CountUp value={walletQuery.data.balance} format={(n) => `${n.toLocaleString("fr-FR")} pts`} />
            ) : undefined,
            hint: `Repart à ${WEEKLY_BANKROLL.toLocaleString("fr-FR")} pts ${formatBankrollReset()}`,
            icon: Wallet,
            isLoading: statsLoading,
          },
          {
            label: "Taux de réussite",
            value: profileQuery.data ? (
              profileQuery.data.totalBets === 0 ? "—" : <CountUp value={profileQuery.data.winRate} format={(n) => `${n}%`} />
            ) : undefined,
            hint: profileQuery.data
              ? profileQuery.data.totalBets === 0
                ? "aucun ticket réglé"
                : `sur ${profileQuery.data.totalBets} ticket${profileQuery.data.totalBets > 1 ? "s" : ""} réglé${profileQuery.data.totalBets > 1 ? "s" : ""}`
              : undefined,
            icon: Target,
            isLoading: statsLoading,
          },
          {
            // Le rang saison est déjà en haut de l'écran : ici, la course de la semaine.
            label: "Points de la semaine",
            value: standing ? formatSignedPoints(standing.weekPoints) : "-",
            hint: standing
              ? standing.weekRank !== null
                ? `#${standing.weekRank} sur ${standing.weekPlayers} cette semaine`
                : "aucun ticket réglé cette semaine"
              : undefined,
            icon: Trophy,
            tone: "paint",
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

      {/* min-w-0 sur les colonnes : un élément de grille ne rétrécit pas sous
          la largeur de son contenu, une ligne trop longue d'une carte élargissait
          toute la colonne au-delà de l'écran (page qui glissait sur téléphone). */}
      <div className="grid gap-6 lg:grid-cols-3">
        <div className="reveal-children flex min-w-0 flex-col gap-6 lg:col-span-2">
          <FavoriteTeamCard
            matches={matchesQuery.data}
            leaderboard={leaderboardQuery.data ?? []}
            username={profileQuery.data?.username}
            favoriteTeamName={profileQuery.data?.favoriteTeam}
          />
          <SeasonPicksNudge />
          <BoostCard matches={matchesQuery.data} />
          <PickOfDay match={slate.spotlight} />
          <SuggestedParlays matches={matchesQuery.data} />
          <UpcomingMatches matches={slate.upcoming} isLoading={matchesQuery.isLoading} />
          <RecentActivity bets={(betsQuery.data ?? []).slice(0, 3)} isLoading={betsQuery.isLoading} />
        </div>

        <div className="reveal-children flex min-w-0 flex-col gap-6">
          <WalletTrend
            series={week.series}
            weeklyDelta={week.net}
            staked={week.staked}
            won={week.won}
            pending={week.pending}
            isLoading={betsQuery.isLoading}
          />
          <LeaderboardPreview
            entries={leaderboardQuery.data ?? []}
            currentUsername={profileQuery.data?.username}
            isLoading={leaderboardQuery.isLoading}
          />
          <LeaguesPreview leagues={leaguesQuery.data ?? []} isLoading={leaguesQuery.isLoading} />
          <NewsPreview items={newsQuery.data ?? []} isLoading={newsQuery.isLoading} />
          <FunFactCard />
        </div>
      </div>
    </div>
  );
}
