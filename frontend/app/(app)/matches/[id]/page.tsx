"use client";

import { use } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { MatchStatusBadge } from "@/components/match-status-badge";
import { TeamRoster } from "@/components/team-roster";
import { fetchMatchById } from "@/lib/api/matches";

export default function MatchDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);

  const { data: match, isLoading, isError } = useQuery({
    queryKey: ["match", id],
    queryFn: () => fetchMatchById(id),
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
        <Card className="border-border bg-card">
          <CardContent className="flex flex-col items-center gap-6 pt-6 text-center">
            <MatchStatusBadge status={match.status} />

            <div className="flex w-full items-center justify-around">
              <div className="flex flex-col items-center gap-2">
                <span className="font-heading text-lg font-bold">
                  {match.awayTeam.name}
                </span>
                <span className="text-xs text-muted-foreground">
                  {match.awayTeam.conference} · {match.awayTeam.division}
                </span>
                {match.status !== "scheduled" && (
                  <span className="font-mono text-3xl font-bold">
                    {match.awayScore}
                  </span>
                )}
              </div>

              <span className="text-muted-foreground">@</span>

              <div className="flex flex-col items-center gap-2">
                <span className="font-heading text-lg font-bold">
                  {match.homeTeam.name}
                </span>
                <span className="text-xs text-muted-foreground">
                  {match.homeTeam.conference} · {match.homeTeam.division}
                </span>
                {match.status !== "scheduled" && (
                  <span className="font-mono text-3xl font-bold">
                    {match.homeScore}
                  </span>
                )}
              </div>
            </div>

            <p className="font-mono text-sm text-muted-foreground">
              {new Date(match.date).toLocaleString("fr-FR", {
                weekday: "long",
                day: "2-digit",
                month: "long",
                hour: "2-digit",
                minute: "2-digit",
              })}
            </p>
          </CardContent>
        </Card>
      )}

      {match && (
        <Card className="border-border bg-card">
          <CardContent className="grid gap-6 pt-6 sm:grid-cols-2">
            <TeamRoster teamId={match.awayTeam.id} teamName={match.awayTeam.name} />
            <TeamRoster teamId={match.homeTeam.id} teamName={match.homeTeam.name} />
          </CardContent>
        </Card>
      )}
    </div>
  );
}