"use client";

import { use } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { MatchStatusBadge } from "@/components/match-status-badge";
import { MatchStageBadge } from "@/components/match-stage-badge";
import { FaceOffTeams, TeamWatermarks, faceOffBackground } from "@/components/match-face-off";
import { NbaLogo } from "@/components/nba-logo";
import { formatMatchTime, getDayLabel } from "@/lib/utils";
import { fetchMatchById } from "@/lib/api/matches";
import type { Match } from "@/lib/types";
import { useLiveStatus, withLiveScore } from "@/lib/live";
import { MatchDetailTabs } from "@/components/match-detail-tabs";
import { MyBetChip } from "@/components/my-bet-chip";

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
              <MatchStatusBadge status={match.status} live={live} />
            </div>
            <MatchStageBadge match={match} className="-mt-2 justify-center" />

            <FaceOffTeams match={match} logoSize={88} morph nameClassName="text-xl" scoreClassName="text-4xl" />
            <MyBetChip match={match} className="mx-auto max-w-full" />

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

      {match && <MatchDetailTabs match={match} tab={onglet} />}
    </div>
  );
}