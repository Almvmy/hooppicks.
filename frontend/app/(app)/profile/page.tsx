"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Share2 } from "lucide-react";
import { fetchProfile } from "@/lib/api/auth";
import { fetchBets } from "@/lib/api/bets";
import { fetchLeaderboard } from "@/lib/api/leaderboard";
import { fetchWallet, fetchWalletTransactions } from "@/lib/api/wallet";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { BadgeGrid } from "@/components/badge-grid";
import { PlayerAvatar } from "@/components/player-avatar";
import { AvatarEditor } from "@/components/avatar-editor";
import { FavoriteTeamPicker } from "@/components/favorite-team-picker";
import { FormStreak } from "@/components/form-streak";
import { BestBetTrophy } from "@/components/best-bet-trophy";
import { CourtWatermark } from "@/components/court-watermark";
import { SeamPattern } from "@/components/seam-pattern";
import { HaloGlow } from "@/components/halo-glow";
import { WeeklyRecapCard } from "@/components/weekly-recap-card";
import { computeWinStreak, findLeaderboardEntry, weeklyWalletDelta } from "@/lib/dashboard";
import { rankTitle } from "@/lib/rank-title";

export default function ProfilePage() {
  const [showRecap, setShowRecap] = useState(true);
  const profileQuery = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const betsQuery = useQuery({ queryKey: ["bets"], queryFn: fetchBets });
  const walletQuery = useQuery({ queryKey: ["wallet"], queryFn: fetchWallet });
  const transactionsQuery = useQuery({
    queryKey: ["wallet-transactions"],
    queryFn: fetchWalletTransactions,
  });
  const leaderboardQuery = useQuery({
    queryKey: ["leaderboard"],
    queryFn: fetchLeaderboard,
    staleTime: 2 * 60 * 1000,
  });

  const profile = profileQuery.data;
  const ownEntry = findLeaderboardEntry(leaderboardQuery.data, profile?.username);
  const title = rankTitle(ownEntry?.rank, leaderboardQuery.data?.length);
  const weeklyDelta = weeklyWalletDelta(transactionsQuery.data);
  const streak = computeWinStreak(betsQuery.data);

  return (
    <div className="flex flex-col gap-6">
      {profileQuery.isError && (
        <p className="text-sm text-destructive">
          Impossible de charger ton profil. Certaines sections ci-dessous peuvent manquer. Réessaie plus tard.
        </p>
      )}

      <div className="glass relative overflow-hidden rounded-2xl p-6">
        <CourtWatermark />
        <div className="relative flex items-center gap-4">
          {profileQuery.isLoading ? (
            <Skeleton className="h-16 w-16 rounded-full" />
          ) : (
            profile && (
              <PlayerAvatar
                number={profile.avatarNumber}
                position={profile.avatarPosition}
                colorway={profile.avatarColorway}
                icon={profile.avatarIcon}
                size="lg"
              />
            )
          )}
          <div>
            {profileQuery.isLoading ? (
              <Skeleton className="h-7 w-40" />
            ) : (
              <h1 className="font-heading text-2xl font-bold">@{profile?.username}</h1>
            )}
            <p className="mt-1 flex items-center gap-2 text-sm">
              <span className="rounded-full border border-primary/30 bg-primary/10 px-2 py-0.5 font-mono text-xs font-bold text-primary">
                {title}
              </span>
              {!leaderboardQuery.isLoading && ownEntry && (
                <span className="text-muted-foreground">
                  #{ownEntry.rank} sur {leaderboardQuery.data?.length} joueurs
                </span>
              )}
            </p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <Card className="relative overflow-hidden">
          <HaloGlow className="left-1/2 top-1/2 h-24 w-24 -translate-x-1/2 -translate-y-1/2 rounded-full opacity-60" />
          <CardContent className="relative pt-6">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Solde actuel</p>
            {walletQuery.isLoading ? (
              <Skeleton className="mt-2 h-7 w-24" />
            ) : walletQuery.isError ? (
              <p className="mt-1 text-sm text-destructive">Indisponible</p>
            ) : (
              <p className="mt-1 font-mono text-2xl font-bold">
                {walletQuery.data?.balance.toLocaleString("fr-FR") ?? "-"} pts
              </p>
            )}
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Taux de réussite</p>
            {profileQuery.isLoading ? (
              <Skeleton className="mt-2 h-7 w-16" />
            ) : (
              <p className="mt-1 font-mono text-2xl font-bold">{profile?.winRate}%</p>
            )}
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Paris joués</p>
            {profileQuery.isLoading ? (
              <Skeleton className="mt-2 h-7 w-12" />
            ) : (
              <p className="mt-1 font-mono text-2xl font-bold">{profile?.totalBets}</p>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="flex flex-col items-start gap-3">
        <div className="flex w-full items-center justify-between">
          <h2 className="font-heading text-lg font-bold">Ta carte de partage</h2>
          <Button
            variant="outline"
            size="sm"
            className="gap-1.5"
            onClick={() => setShowRecap((v) => !v)}
          >
            <Share2 className="h-4 w-4" />
            {showRecap ? "Masquer" : "Afficher"}
          </Button>
        </div>

        {showRecap && profile && (
          <WeeklyRecapCard
            username={profile.username}
            weeklyDelta={weeklyDelta}
            rank={ownEntry?.rank}
            totalPlayers={leaderboardQuery.data?.length}
            winRate={profile.winRate}
            streak={streak}
            seasonLabel="Saison 2026"
          />
        )}
      </div>

      <Card>
        <CardContent className="flex flex-col gap-2 pt-6">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Forme du moment</p>
          <FormStreak bets={betsQuery.data} />
        </CardContent>
      </Card>

      <BestBetTrophy bets={betsQuery.data} />

      {profile && <AvatarEditor profile={profile} />}

      <Card>
        <CardContent className="flex flex-col gap-4 pt-6">
          <div>
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Informations</p>
            <p className="mt-2 text-sm">
              E-mail : <span className="text-muted-foreground">{profile?.email}</span>
            </p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Équipe favorite</p>
            <div className="mt-2">
              {profile && <FavoriteTeamPicker currentTeam={profile.favoriteTeam} />}
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="relative overflow-hidden rounded-lg">
        <SeamPattern className="pointer-events-none absolute -right-6 -top-6 h-[160px] w-[220px] opacity-[0.05]" />
        <div className="relative mb-3 flex items-center justify-between">
          <h2 className="font-heading text-lg font-bold">Badges</h2>
          <Link href="/bets" className="text-xs font-medium text-primary hover:underline">
            Voir mon historique
          </Link>
        </div>
        <div className="relative">
          <BadgeGrid />
        </div>
      </div>
    </div>
  );
}
