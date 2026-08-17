"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, Trophy } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchLeaderboard } from "@/lib/mock/leaderboard";
import { fetchProfile } from "@/lib/api/auth";
import { cn } from "@/lib/utils";

const RANK_COLORS: Record<number, string> = {
  1: "text-primary",
  2: "text-muted-foreground",
  3: "text-muted-foreground",
};

export function MiniLeaderboardCard() {
  const leaderboardQuery = useQuery({ queryKey: ["leaderboard-dashboard"], queryFn: fetchLeaderboard });
  const profileQuery = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });

  const top = (leaderboardQuery.data ?? []).slice(0, 5);
  const isLoading = leaderboardQuery.isLoading || profileQuery.isLoading;

  return (
    <Card className="border-border bg-card">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="flex items-center gap-2 font-heading text-lg">
          <Trophy className="h-4 w-4 text-primary" />
          Classement
        </CardTitle>
        <Link
          href="/leaderboard"
          className="flex items-center gap-1 text-xs font-medium text-muted-foreground transition-colors hover:text-primary"
        >
          Tout voir
          <ArrowRight className="h-3 w-3" />
        </Link>
      </CardHeader>
      <CardContent className="flex flex-col gap-1.5">
        {isLoading &&
          Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-8 w-full rounded-md" />)}

        {leaderboardQuery.isError && (
          <p className="text-sm text-destructive">Impossible de charger le classement.</p>
        )}

        {!isLoading &&
          !leaderboardQuery.isError &&
          top.map((entry) => {
            const isMe = entry.username === profileQuery.data?.username;
            return (
              <div
                key={entry.rank}
                className={cn(
                  "flex items-center justify-between rounded-md px-2 py-1.5 text-sm",
                  isMe && "bg-primary/10 ring-1 ring-primary/20"
                )}
              >
                <div className="flex items-center gap-2">
                  <span className={cn("w-5 font-mono text-xs font-bold", RANK_COLORS[entry.rank])}>
                    #{entry.rank}
                  </span>
                  <span className={cn("font-medium", isMe && "text-primary")}>
                    {entry.username}
                    {isMe && <span className="ml-1 text-xs text-muted-foreground">(toi)</span>}
                  </span>
                </div>
                <span className="font-mono text-xs text-muted-foreground">
                  {entry.points.toLocaleString("fr-FR")} pts
                </span>
              </div>
            );
          })}
      </CardContent>
    </Card>
  );
}
