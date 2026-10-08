"use client";

import { use } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { MatchStatusBadge } from "@/components/match-status-badge";
import { MatchStageBadge } from "@/components/match-stage-badge";
import { TeamRoster } from "@/components/team-roster";
import { MatchBoxScore } from "@/components/match-box-score";
import { FaceOffTeams, TeamWatermarks, faceOffBackground } from "@/components/match-face-off";
import { NbaLogo } from "@/components/nba-logo";
import { TeamNews } from "@/components/news/team-news";
import { bettingClosedReason, formatMatchTime, getDayLabel, isBettable } from "@/lib/utils";
import { MatchOddsRow } from "@/components/match-odds-row";
import { PlayerPropsCard } from "@/components/player-props-card";
import { fetchMatchById } from "@/lib/api/matches";
import type { Match } from "@/lib/types";

export default function MatchDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);

  const queryClient = useQueryClient();

  const { data: match, isLoading, isError } = useQuery({
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
        <Card style={faceOffBackground(match, 18)} className="relative overflow-hidden">
          <TeamWatermarks match={match} size={320} opacity={0.12} />
          <CardContent className="relative flex flex-col gap-5 pt-6">
            <div className="flex items-center justify-center gap-2">
              <NbaLogo size={24} />
              <MatchStatusBadge status={match.status} />
            </div>
            <MatchStageBadge match={match} className="-mt-2 justify-center" />

            <FaceOffTeams match={match} logoSize={88} morph nameClassName="text-xl" scoreClassName="text-4xl" />

            <div className="grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] gap-3 text-center text-xs text-muted-foreground">
              <span>{match.awayTeam.conference} · {match.awayTeam.division}</span>
              <span />
              <span>{match.homeTeam.conference} · {match.homeTeam.division}</span>
            </div>

            <p className="text-center font-mono text-sm text-muted-foreground">
              {getDayLabel(new Date(match.date))} · {formatMatchTime(new Date(match.date))}
            </p>
          </CardContent>
        </Card>
      )}

      {/* Les cotes juste sous l'affiche : c'est ici qu'on arrive depuis une
          carte de match, une actu ou la page équipe, et la fiche ne
          permettait pas de parier. */}
      {match && isBettable(match) && (
        <Card>
          <CardContent className="pt-6">
            <h2 className="font-heading text-base font-bold">Parier sur ce match</h2>
            <p className="text-xs text-muted-foreground">
              Vainqueur, écart ou total : une sélection par match et par ticket.
            </p>
            <MatchOddsRow match={match} />
          </CardContent>
        </Card>
      )}

      {match && isBettable(match) && <PlayerPropsCard match={match} />}

      {match && match.status === "scheduled" && !isBettable(match) && (
        <p className="glass-inset-quiet rounded-xl px-3 py-2 text-center text-sm text-muted-foreground first-letter:uppercase">
          {bettingClosedReason(match)}.
        </p>
      )}

      {match && match.status !== "finished" && (
        <TeamNews abbreviations={[match.awayTeam.abbreviation, match.homeTeam.abbreviation]} />
      )}

      {match && match.status === "finished" && (
        <Card>
          <CardContent className="pt-6">
            <h2 className="mb-4 font-heading text-base font-bold">Feuille de match</h2>
            <MatchBoxScore
              matchId={match.id}
              homeTeam={match.homeTeam}
              awayTeam={match.awayTeam}
            />
          </CardContent>
        </Card>
      )}

      {match && match.status !== "finished" && (
        <Card>
          <CardContent className="grid gap-6 pt-6 sm:grid-cols-2">
            <TeamRoster team={match.awayTeam} />
            <TeamRoster team={match.homeTeam} />
          </CardContent>
        </Card>
      )}
    </div>
  );
}