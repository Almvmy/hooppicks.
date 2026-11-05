"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Swords } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { LeagueCrest } from "@/components/league-crest";
import { fetchLeagueRanking } from "@/lib/api/leagues";
import { useLeaderboardPeriods } from "@/lib/leaderboard-periods";
import type { LeaderboardPeriod } from "@/lib/types";
import { cn, formatSignedPoints } from "@/lib/utils";

/** Ligue contre ligue (LeagueRankingService) : moyenne des 5 meilleurs joueurs de chaque ligue. */
export function LeagueRanking() {
  const [period, setPeriod] = useState<LeaderboardPeriod>("week");
  const periods = useLeaderboardPeriods();
  const { data, isLoading, isError } = useQuery({
    queryKey: ["league-ranking", period],
    queryFn: () => fetchLeagueRanking(period),
    staleTime: 2 * 60_000,
  });

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-5">
        <div>
          <h2 className="flex items-center gap-2 font-heading text-base font-bold">
            <Swords className="h-4 w-4 text-primary" />
            Ligue contre ligue
          </h2>
          <p className="text-xs text-muted-foreground">Moyenne des 5 meilleurs joueurs de chaque ligue.</p>
        </div>

        <div className="glass-scroll flex gap-2 overflow-x-auto" role="group" aria-label="Période">
          {periods.map((p) => (
            <button
              key={p.value}
              type="button"
              aria-pressed={period === p.value}
              onClick={() => setPeriod(p.value)}
              className={cn(
                "shrink-0 rounded-full px-3 py-1 text-xs transition-colors",
                period === p.value ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
              )}
            >
              {p.label}
            </button>
          ))}
        </div>

        {isLoading && <Skeleton className="h-24 w-full rounded-xl" />}
        {isError && <p className="text-sm text-destructive">Impossible de charger le classement des ligues.</p>}
        {data?.length === 0 && (
          <p className="py-2 text-sm text-muted-foreground">Il faut au moins 3 membres pour qu&apos;une ligue soit classée.</p>
        )}

        <div className="flex flex-col divide-y divide-tint/10">
          {data?.map((l) => (
            <div key={l.id} className={cn("flex items-center gap-3 py-2", l.mine && "font-semibold")}>
              <span className="w-7 shrink-0 font-mono text-xs text-muted-foreground">#{l.rank}</span>
              <LeagueCrest name={l.name} size={28} />
              <span className="min-w-0 flex-1 truncate text-sm">
                {l.name}
                {l.mine && <span className="ml-1.5 text-[11px] text-primary">ta ligue</span>}
              </span>
              <span className="shrink-0 text-[11px] font-normal text-muted-foreground">{l.members} membres</span>
              <span className="shrink-0 font-mono text-sm font-bold">{formatSignedPoints(l.score, false)}</span>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
