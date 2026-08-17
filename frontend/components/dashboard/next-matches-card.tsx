"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, CalendarDays } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchMatches } from "@/lib/mock/matches";

export function NextMatchesCard() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["matches-dashboard"],
    queryFn: fetchMatches,
  });

  const upcoming = (data ?? [])
    .filter((m) => m.status === "scheduled")
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
    .slice(0, 3);

  return (
    <Card className="border-border bg-card">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="flex items-center gap-2 font-heading text-lg">
          <CalendarDays className="h-4 w-4 text-primary" />
          Prochains matchs
        </CardTitle>
        <Link
          href="/matches"
          className="flex items-center gap-1 text-xs font-medium text-muted-foreground transition-colors hover:text-primary"
        >
          Tout voir
          <ArrowRight className="h-3 w-3" />
        </Link>
      </CardHeader>
      <CardContent className="flex flex-col gap-2">
        {isLoading &&
          Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-14 w-full rounded-lg" />)}

        {isError && <p className="text-sm text-destructive">Impossible de charger les matchs.</p>}

        {!isLoading && !isError && upcoming.length === 0 && (
          <p className="text-sm text-muted-foreground">Aucun match à venir pour l&apos;instant.</p>
        )}

        {!isLoading &&
          !isError &&
          upcoming.map((match) => {
            const date = new Date(match.date);
            return (
              <Link
                key={match.id}
                href={`/matches/${match.id}`}
                className="flex items-center justify-between rounded-lg border border-border/60 bg-secondary/20 px-3 py-2.5 text-sm transition-colors hover:border-primary/40 hover:bg-secondary/40"
              >
                <div>
                  <p className="font-medium">
                    {match.awayTeam.abbreviation} @ {match.homeTeam.abbreviation}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {match.awayTeam.name} vs {match.homeTeam.name}
                  </p>
                </div>
                <div className="text-right">
                  <p className="font-mono text-xs font-medium">
                    {date.toLocaleDateString("fr-FR", { day: "2-digit", month: "short" })}
                  </p>
                  <p className="font-mono text-xs text-muted-foreground">
                    {date.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })}
                  </p>
                </div>
              </Link>
            );
          })}
      </CardContent>
    </Card>
  );
}
