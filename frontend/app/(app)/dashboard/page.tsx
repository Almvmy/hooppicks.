"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";

import { fetchProfile } from "@/lib/api/auth";
import { fetchWallet } from "@/lib/api/wallet";
import { fetchBets } from "@/lib/api/bets";
import { DEFAULT_PAST_DAYS, fetchMatches } from "@/lib/api/matches";
import { fetchLeaderboard, fetchMyStanding } from "@/lib/api/leaderboard";
import { fetchNews } from "@/lib/api/news";

import { buildDashboardSlate, computeWinStreak, pendingBetsSummary, weekSummary } from "@/lib/dashboard";

import { WeekCard } from "@/components/dashboard/week-card";
import { LiveTickets } from "@/components/dashboard/live-tickets";
import { CompetitionCard } from "@/components/dashboard/competition-card";
import { PickOfDay } from "@/components/dashboard/pick-of-day";
import { UpcomingMatches } from "@/components/dashboard/upcoming-matches";
import { RecentActivity } from "@/components/dashboard/recent-activity";
import { NewsPreview } from "@/components/dashboard/news-preview";
import { FavoriteTeamCard } from "@/components/dashboard/favorite-team-card";
import { SeasonPicksNudge } from "@/components/dashboard/season-picks-nudge";
import { AppNudges } from "@/components/app-nudges";
import { BoostCard } from "@/components/dashboard/boost-card";
import { SeasonCountdownCard } from "@/components/dashboard/season-countdown-card";
import { SeasonStoryAutoOpen } from "@/components/season-story";
import { SuggestedParlays } from "@/components/dashboard/suggested-parlays";
import { useBoost } from "@/lib/boost";

export default function DashboardPage() {
  const profileQuery = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const walletQuery = useQuery({ queryKey: ["wallet"], queryFn: fetchWallet });
  const standingQuery = useQuery({ queryKey: ["leaderboard", "me"], queryFn: fetchMyStanding, refetchInterval: 60_000 });
  const betsQuery = useQuery({ queryKey: ["bets"], queryFn: fetchBets });
  const matchesQuery = useQuery({
    queryKey: ["matches", "window", DEFAULT_PAST_DAYS],
    queryFn: () => fetchMatches(),
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

  const streak = computeWinStreak(betsQuery.data);
  const pending = pendingBetsSummary(betsQuery.data);
  const boost = useBoost();
  // Le match boosté a sa propre carte : pas une seconde fois « à la une »
  // (sauf en direct, où la une sert à suivre le score). Un direct sur lequel
  // on a un ticket est déjà dans « Tes matchs en direct ».
  const myLiveIds = new Set(
    (betsQuery.data ?? []).filter((b) => b.status === "pending").flatMap((b) => b.selections.map((s) => s.matchId))
  );
  const slate = buildDashboardSlate(
    matchesQuery.data?.filter(
      (m) => (m.id !== boost?.matchId || m.status === "live") && !(m.status === "live" && myLiveIds.has(m.id))
    )
  );
  const week = weekSummary(betsQuery.data);
  const standing = standingQuery.data;

  // Tickets réglés depuis la veille au soir : au réveil, les résultats de la
  // nuit passent avant les paris du jour.
  const [now] = useState(() => Date.now());
  const freshResults = (betsQuery.data ?? []).some(
    (b) => b.status !== "pending" && b.resolvedAt && now - new Date(b.resolvedAt).getTime() < 14 * 3600 * 1000
  );
  const recent = <RecentActivity bets={(betsQuery.data ?? []).slice(0, 3)} isLoading={betsQuery.isLoading} />;

  return (
    <div className="flex flex-col gap-6">
      <WeekCard
        username={profileQuery.data?.username}
        standing={standing}
        balance={walletQuery.data?.balance}
        series={week.series}
        pendingCount={pending.count}
        pendingStake={pending.stake}
        streak={streak}
        isLoading={profileQuery.isLoading || standingQuery.isLoading}
      />

      <LiveTickets matches={matchesQuery.data} bets={betsQuery.data} />

      <SeasonCountdownCard matches={matchesQuery.data} />
      <SeasonStoryAutoOpen />

      {/* min-w-0 sur les colonnes : un élément de grille ne rétrécit pas sous
          la largeur de son contenu, une ligne trop longue d'une carte élargissait
          toute la colonne au-delà de l'écran (page qui glissait sur téléphone). */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="reveal-children flex min-w-0 flex-col gap-6 lg:col-span-2">
          <AppNudges hasBets={(betsQuery.data?.length ?? 0) > 0} />
          {freshResults && recent}
          <SeasonPicksNudge />
          <BoostCard matches={matchesQuery.data} />
          <PickOfDay match={slate.spotlight} />
          <SuggestedParlays matches={matchesQuery.data} />
          <UpcomingMatches matches={slate.upcoming} isLoading={matchesQuery.isLoading} />
        </div>

        <div className="reveal-children flex min-w-0 flex-col gap-6">
          <CompetitionCard standing={standing} username={profileQuery.data?.username} />
          {!freshResults && recent}
          <FavoriteTeamCard
            matches={matchesQuery.data}
            leaderboard={leaderboardQuery.data ?? []}
            username={profileQuery.data?.username}
            favoriteTeamName={profileQuery.data?.favoriteTeam}
          />
          <NewsPreview items={newsQuery.data ?? []} isLoading={newsQuery.isLoading} />
        </div>
      </div>
    </div>
  );
}
