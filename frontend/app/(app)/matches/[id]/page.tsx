"use client";

import { use } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { MatchStatusBadge } from "@/components/match-status-badge";
import { MatchStageBadge } from "@/components/match-stage-badge";
import { TeamWatermarks, faceOffBackground } from "@/components/match-face-off";
import { MorphingTeamLogo, teamLogoMorphName } from "@/components/team-logo";
import { NbaLogo } from "@/components/nba-logo";
import { cn, formatMatchTime, getDayLabel } from "@/lib/utils";
import { fetchMatchById } from "@/lib/api/matches";
import type { Match, TeamRank } from "@/lib/types";
import { type LiveStatus, useLiveStatus, withLiveScore } from "@/lib/live";
import { useTeamsByAbbreviation } from "@/lib/use-teams";
import { MatchDetailTabs } from "@/components/match-detail-tabs";
import { MyBetChip } from "@/components/my-bet-chip";
import { FollowMatchButton } from "@/components/follow-match-button";

/** Une équipe de l'affiche : logo, nom, bilan de la saison, absents. Jamais grisée : seul le score du perdant l'est. */
function HeroTeam({ match, side, rank }: { match: Match; side: "away" | "home"; rank: TeamRank | undefined }) {
  const team = side === "away" ? match.awayTeam : match.homeTeam;
  return (
    <div className="flex min-w-0 flex-col items-center gap-1.5 text-center">
      <MorphingTeamLogo morphName={teamLogoMorphName(match.id, side)} abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={56} />
      {/* Sigle sur téléphone : avec le score en grand, « Timberwolves » ne tenait plus. */}
      <span className="max-w-full truncate font-heading text-base font-bold leading-tight">
        <span className="sm:hidden">{team.abbreviation}</span>
        <span className="hidden sm:inline">{team.name}</span>
      </span>
      {rank?.wins != null && rank.losses != null && (
        <span className="font-mono text-[11px] text-muted-foreground">
          {rank.wins}-{rank.losses}
        </span>
      )}
      {/* Masqué en présaison : le rapport de blessures n'est pas encore à jour. */}
      {!!team.outPlayersCount && match.type !== "preseason" && (
        <span className="text-[10px] text-muted-foreground" title={`${team.outPlayersCount} joueur(s) indisponible(s)`}>
          {team.outPlayersCount} absent{team.outPlayersCount > 1 ? "s" : ""}
        </span>
      )}
    </div>
  );
}

/** Au centre : l'heure du coup d'envoi avant le match, le score en grand ensuite (le vainqueur ressort). */
function HeroCenter({ match, live }: { match: Match; live: LiveStatus | undefined }) {
  const date = new Date(match.date);
  if (match.status === "scheduled") {
    return (
      <div className="flex flex-col items-center gap-0.5">
        <span className="text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">{getDayLabel(date)}</span>
        <span className="font-heading text-3xl font-bold tabular-nums leading-none">{formatMatchTime(date)}</span>
      </div>
    );
  }
  const away = match.awayScore ?? 0;
  const home = match.homeScore ?? 0;
  const dim = (mine: number, theirs: number) => match.status === "finished" && mine < theirs && "text-muted-foreground";
  return (
    <div className="flex flex-col items-center gap-1.5">
      <span className="flex items-baseline gap-1.5 whitespace-nowrap font-heading text-[40px] font-bold tabular-nums leading-none sm:gap-2 sm:text-5xl">
        <span className={cn(dim(away, home))}>{away}</span>
        <span className="text-xl text-muted-foreground sm:text-2xl">–</span>
        <span className={cn(dim(home, away))}>{home}</span>
      </span>
      <MatchStatusBadge status={match.status} live={live} />
    </div>
  );
}

export default function MatchDetailPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ onglet?: string }>;
}) {
  const { id } = use(params);
  const { onglet } = use(searchParams);

  const queryClient = useQueryClient();

  const { data: synced, isLoading, isError } = useQuery({
    queryKey: ["match", id],
    queryFn: () => fetchMatchById(id),
    refetchInterval: 60 * 1000,
    // Arrivée depuis la liste : le match y est déjà, on l'affiche tout de
    // suite (puis on le rafraîchit en arrière-plan) au lieu d'un squelette.
    // C'est aussi ce qui permet au logo d'exister dès la transition de page,
    // donc de "voler" depuis la carte au lieu d'apparaître après coup.
    initialData: () => queryClient.getQueryData<Match[]>(["matches"])?.find((m) => m.id === id),
    initialDataUpdatedAt: () => queryClient.getQueryState(["matches"])?.dataUpdatedAt,
  });
  // Direct ESPN (affichage seulement) : sans lui, on garde le match de la synchro tel quel.
  const live = useLiveStatus(synced);
  const match = synced && withLiveScore(synced, live);
  const teams = useTeamsByAbbreviation();

  return (
    <div className="flex flex-col gap-6">
      <Link
        href="/matches"
        className="flex w-fit items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" />
        Retour aux matchs
      </Link>

      {isLoading && <Skeleton className="h-40 w-full rounded-lg" />}

      {isError && (
        <p className="text-destructive">Impossible de charger ce match.</p>
      )}

      {!isLoading && !isError && !match && (
        <p className="text-muted-foreground">Ce match n&apos;existe pas.</p>
      )}

      {match && (
        // overflow-clip (si le navigateur le connaît) plutôt que hidden : les
        // filigranes dépassent, et avec hidden, mettre le focus sur la cloche
        // faisait défiler l'en-tête de côté.
        <Card
          size="sm"
          style={faceOffBackground(match, 30)}
          className="relative overflow-hidden supports-[overflow:clip]:overflow-clip"
        >
          <TeamWatermarks match={match} size={260} opacity={0.14} />
          {/* En-tête plutôt rectangulaire : statut et cloche sur une même ligne,
              logos plus petits, sans la ligne conférence · division (déjà sur la
              page de l'équipe). Carré et haut, il poussait les onglets hors de
              l'écran du téléphone. */}
          <CardContent className="relative flex flex-col gap-2">
            <div className="flex items-center justify-between gap-2">
              <div className="flex min-w-0 items-center gap-2">
                <NbaLogo size={22} />
                <MatchStageBadge match={match} />
              </div>
              <FollowMatchButton match={match} className="h-9 w-9 shrink-0" />
            </div>
            <div className="grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-3">
              <HeroTeam match={match} side="away" rank={teams.get(match.awayTeam.abbreviation)} />
              <HeroCenter match={match} live={live} />
              <HeroTeam match={match} side="home" rank={teams.get(match.homeTeam.abbreviation)} />
            </div>
            <MyBetChip match={match} className="mx-auto max-w-full" />
            {match.status !== "scheduled" && (
              <p className="text-center font-mono text-[11px] text-muted-foreground">
                {getDayLabel(new Date(match.date))} · {formatMatchTime(new Date(match.date))}
              </p>
            )}
          </CardContent>
        </Card>
      )}

      {match && <MatchDetailTabs match={match} tab={onglet} />}
    </div>
  );
}