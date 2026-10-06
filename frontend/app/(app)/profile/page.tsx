"use client";

import { createElement, useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Eye, EyeOff, Flame, History, Medal, Settings, UserRound } from "lucide-react";
import { fetchProfile } from "@/lib/api/auth";
import { fetchBets } from "@/lib/api/bets";
import { fetchBadges } from "@/lib/api/badges";
import { fetchLeaderboard, fetchMyStanding } from "@/lib/api/leaderboard";
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
import { WeeklyRecapCard } from "@/components/weekly-recap-card";
import { TeamLogo } from "@/components/team-logo";
import { CountUp } from "@/components/motion/count-up";
import { badgeIcon } from "@/lib/badges";
import { netResult } from "@/lib/bet-legs";
import { computeWinStreak, findLeaderboardEntry } from "@/lib/dashboard";
import { rankTitle } from "@/lib/rank-title";
import { getTeamColor } from "@/lib/team-colors";
import { favoriteTeamAbbreviation, useTeamsByAbbreviation } from "@/lib/use-teams";
import { cn, formatSignedPoints, seasonLabel } from "@/lib/utils";

function StatTile({
  label,
  loading,
  children,
  hint,
  className,
}: {
  label: string;
  loading: boolean;
  children: React.ReactNode;
  hint?: string;
  className?: string;
}) {
  return (
    <Card className={className}>
      <CardContent className="pt-5">
        <p className="text-[11px] uppercase tracking-wide text-muted-foreground">{label}</p>
        {loading ? (
          <Skeleton className="mt-2 h-7 w-20" />
        ) : (
          <p className="mt-1 font-heading text-2xl font-bold">{children}</p>
        )}
        {hint && !loading && <p className="text-xs text-muted-foreground">{hint}</p>}
      </CardContent>
    </Card>
  );
}

export default function ProfilePage() {
  const [showRecap, setShowRecap] = useState(true);
  const profileQuery = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const betsQuery = useQuery({ queryKey: ["bets"], queryFn: fetchBets });
  const standingQuery = useQuery({ queryKey: ["leaderboard", "me"], queryFn: fetchMyStanding });
  const badgesQuery = useQuery({ queryKey: ["badges"], queryFn: fetchBadges });
  const leaderboardQuery = useQuery({
    queryKey: ["leaderboard"],
    queryFn: () => fetchLeaderboard(),
    staleTime: 2 * 60 * 1000,
  });
  const teams = useTeamsByAbbreviation();

  const profile = profileQuery.data;
  const ownEntry = findLeaderboardEntry(leaderboardQuery.data, profile?.username);
  const title = rankTitle(ownEntry?.rank, leaderboardQuery.data?.length);
  // Bénéfice net de la semaine de jeu, pas l'écart de solde : le solde
  // remonte à 1 000 chaque lundi.
  const weeklyDelta = standingQuery.data?.weekPoints ?? 0;
  const streak = computeWinStreak(betsQuery.data);
  const net = (betsQuery.data ?? []).reduce((sum, b) => sum + netResult(b), 0);

  // L'en-tête prend les couleurs de l'équipe favorite (orange de marque sinon).
  const favorite = favoriteTeamAbbreviation(profile?.favoriteTeam, teams);
  const favoriteTeam = favorite ? teams.get(favorite) : undefined;
  const heroColor = favorite ? getTeamColor(favorite) : "var(--brand)";

  const badges = badgesQuery.data ?? [];
  const unlocked = badges.filter((b) => b.unlocked).length;
  const nextBadge = badges.find((b) => !b.unlocked);

  return (
    <div className="flex flex-col gap-6">
      {profileQuery.isError && (
        <p className="text-sm text-destructive">
          Impossible de charger ton profil. Certaines sections ci-dessous peuvent manquer. Réessaie plus tard.
        </p>
      )}

      {/* ── En-tête ─────────────────────────────────────────────────── */}
      <div
        className="glass relative overflow-hidden rounded-3xl p-6 sm:p-8"
        style={{
          backgroundImage: `linear-gradient(115deg, color-mix(in srgb, ${heroColor} 22%, transparent) 0%, transparent 65%), var(--glass-tint)`,
        }}
      >
        <CourtWatermark />
        {favoriteTeam && (
          <div aria-hidden className="pointer-events-none absolute -right-10 top-1/2 -translate-y-1/2 rotate-12 opacity-[0.12]">
            <TeamLogo abbreviation={favoriteTeam.abbreviation} logoUrl={favoriteTeam.logoUrl} size={240} />
          </div>
        )}

        <div className="relative flex flex-wrap items-center gap-5">
          {profileQuery.isLoading ? (
            <Skeleton className="h-[72px] w-[72px] rounded-full" />
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
          <div className="min-w-[10rem] flex-1">
            {profileQuery.isLoading ? (
              <Skeleton className="h-8 w-44" />
            ) : (
              <h1 className="truncate font-heading text-3xl font-bold">@{profile?.username}</h1>
            )}
            <div className="mt-2 flex flex-wrap items-center gap-2 text-sm">
              <span className="rounded-full border border-primary/30 bg-primary/10 px-2.5 py-0.5 font-mono text-xs font-bold text-primary">
                {title}
              </span>
              {!leaderboardQuery.isLoading && ownEntry && (
                <span className="text-muted-foreground">
                  #{ownEntry.rank} sur {leaderboardQuery.data?.length} joueurs
                </span>
              )}
              {favoriteTeam && (
                <span className="flex items-center gap-1.5 text-muted-foreground">
                  <TeamLogo abbreviation={favoriteTeam.abbreviation} logoUrl={favoriteTeam.logoUrl} size={20} />
                  {profile?.favoriteTeam}
                </span>
              )}
            </div>
          </div>

          {/* Pleine largeur sur mobile : passe sous le pseudo au lieu de l'écraser. */}
          <div className="flex w-full flex-wrap gap-2 lg:w-auto">
            {profile && (
              <Link href={`/u/${encodeURIComponent(profile.username)}`}>
                <Button variant="outline" size="sm">
                  <UserRound className="h-3.5 w-3.5" />
                  Mon profil public
                </Button>
              </Link>
            )}
            <Link href="/profile/history">
              <Button variant="outline" size="sm">
                <History className="h-3.5 w-3.5" />
                Historique
              </Button>
            </Link>
            <Link href="/settings">
              <Button variant="ghost" size="sm" aria-label="Paramètres">
                <Settings className="h-4 w-4" />
              </Button>
            </Link>
          </div>
        </div>
      </div>

      {/* ── Chiffres ────────────────────────────────────────────────── */}
      <div className="stagger-children grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatTile
          label="Points de classement"
          loading={standingQuery.isLoading}
          hint={
            standingQuery.data?.seasonRank != null
              ? `#${standingQuery.data.seasonRank} sur ${standingQuery.data.seasonPlayers} cette saison`
              : "bénéfice net de la saison"
          }
        >
          {standingQuery.isError ? (
            <span className="text-sm text-destructive">Indisponible</span>
          ) : (
            <CountUp value={standingQuery.data?.seasonPoints ?? 0} format={(n) => formatSignedPoints(n)} />
          )}
        </StatTile>
        <StatTile label="Réussite" loading={profileQuery.isLoading} hint={`sur ${profile?.totalBets ?? 0} paris`}>
          <CountUp value={profile?.winRate ?? 0} format={(n) => `${n}%`} />
        </StatTile>
        <StatTile label="Série en cours" loading={betsQuery.isLoading} hint={streak > 0 ? "tickets gagnés d'affilée" : "aucune série"}>
          <span className="flex items-center gap-1.5">
            {streak > 0 && <Flame className="h-5 w-5 text-primary" />}
            <CountUp value={streak} />
          </span>
        </StatTile>
        <StatTile label="Bilan net" loading={betsQuery.isLoading} hint="gains moins mises">
          <span className={cn(net > 0 ? "text-success" : net < 0 ? "text-destructive" : undefined)}>
            <CountUp value={net} format={(n) => `${n > 0 ? "+" : ""}${n.toLocaleString("fr-FR")}`} />
          </span>
        </StatTile>
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,420px)]">
        {/* ── Colonne principale : jeu ──────────────────────────────── */}
        <div className="flex min-w-0 flex-col gap-6">
          <Card>
            <CardContent className="flex flex-col gap-2 pt-6">
              <p className="text-xs uppercase tracking-wide text-muted-foreground">Forme du moment</p>
              <FormStreak bets={betsQuery.data} />
            </CardContent>
          </Card>

          <BestBetTrophy bets={betsQuery.data} />

          <div className="relative">
            <SeamPattern className="pointer-events-none absolute -right-6 -top-6 h-[160px] w-[220px] opacity-[0.05]" />
            <div className="relative mb-3 flex flex-wrap items-end justify-between gap-3">
              <h2 className="flex items-center gap-2 font-heading text-lg font-bold">
                <Medal className="h-5 w-5 text-paint" />
                Badges
              </h2>
              {badges.length > 0 && (
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <div className="h-1.5 w-28 overflow-hidden rounded-full bg-tint/10">
                    <div
                      className="h-full rounded-full bg-paint transition-[width] duration-700"
                      style={{ width: `${(unlocked / badges.length) * 100}%` }}
                    />
                  </div>
                  {unlocked}/{badges.length}
                </div>
              )}
            </div>
            {nextBadge && (
              <div className="glass-inset-quiet relative mb-3 flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm">
                {createElement(badgeIcon(nextBadge.icon), { className: "h-5 w-5 shrink-0 text-paint" })}
                <span>
                  <span className="font-medium">Prochain objectif : {nextBadge.label}.</span>{" "}
                  <span className="text-muted-foreground">{nextBadge.description}</span>
                </span>
              </div>
            )}
            <div className="relative">
              <BadgeGrid />
            </div>
          </div>
        </div>

        {/* ── Colonne latérale : identité ───────────────────────────── */}
        <div className="flex min-w-0 flex-col gap-6">
          <div className="flex flex-col items-start gap-3">
            <div className="flex w-full items-center justify-between">
              <h2 className="font-heading text-lg font-bold">Ta carte de partage</h2>
              <Button variant="ghost" size="sm" className="gap-1.5" onClick={() => setShowRecap((v) => !v)}>
                {showRecap ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
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
                seasonLabel={`Saison ${seasonLabel()}`}
              />
            )}
          </div>

          {profile && <AvatarEditor profile={profile} />}

          <Card>
            <CardContent className="flex flex-col gap-4 pt-6">
              <div>
                <p className="text-xs uppercase tracking-wide text-muted-foreground">Équipe favorite</p>
                <div className="mt-2 flex items-center gap-3">
                  {favoriteTeam && (
                    <TeamLogo abbreviation={favoriteTeam.abbreviation} logoUrl={favoriteTeam.logoUrl} size={32} />
                  )}
                  {profile && <FavoriteTeamPicker currentTeam={profile.favoriteTeam} />}
                </div>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-muted-foreground">E-mail</p>
                <p className="mt-1 text-sm text-muted-foreground">{profile?.email}</p>
                <Link href="/settings" className="mt-1 inline-block text-xs font-medium text-primary hover:underline">
                  Modifier dans les paramètres
                </Link>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
