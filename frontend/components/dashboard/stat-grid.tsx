"use client";

import { useQuery } from "@tanstack/react-query";
import { Flame, Percent, Snowflake, Ticket, Trophy } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchProfile } from "@/lib/api/auth";
import { fetchBets } from "@/lib/mock/bets";
import { fetchLeaderboard } from "@/lib/mock/leaderboard";
import { computeStreak } from "@/lib/dashboard";
import { cn } from "@/lib/utils";

interface StatItem {
  label: string;
  value: string;
  icon: React.ElementType;
  accentClassName: string;
  isLoading: boolean;
  isError: boolean;
}

export function StatGrid() {
  const profileQuery = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const betsQuery = useQuery({ queryKey: ["bets"], queryFn: fetchBets });
  const leaderboardQuery = useQuery({ queryKey: ["leaderboard"], queryFn: fetchLeaderboard });

  const pendingCount = betsQuery.data?.filter((b) => b.status === "pending").length ?? 0;
  const streak = betsQuery.data ? computeStreak(betsQuery.data) : { count: 0, kind: "none" as const };
  const myRank = leaderboardQuery.data?.find((e) => e.username === profileQuery.data?.username)?.rank;

  const streakLabel =
    streak.kind === "none"
      ? "—"
      : `${streak.count} ${streak.kind === "won" ? "victoire" : "défaite"}${streak.count > 1 ? "s" : ""}`;

  const items: StatItem[] = [
    {
      label: "Taux de réussite",
      value: profileQuery.data ? `${profileQuery.data.winRate}%` : "—",
      icon: Percent,
      accentClassName: "bg-primary/10 text-primary",
      isLoading: profileQuery.isLoading,
      isError: profileQuery.isError,
    },
    {
      label: "Série en cours",
      value: streakLabel,
      icon: streak.kind === "lost" ? Snowflake : Flame,
      accentClassName:
        streak.kind === "lost" ? "bg-secondary text-muted-foreground" : "bg-success/10 text-success",
      isLoading: betsQuery.isLoading,
      isError: betsQuery.isError,
    },
    {
      label: "Paris en attente",
      value: String(pendingCount),
      icon: Ticket,
      accentClassName: "bg-primary/10 text-primary",
      isLoading: betsQuery.isLoading,
      isError: betsQuery.isError,
    },
    {
      label: "Rang au classement",
      value: myRank ? `#${myRank}` : "—",
      icon: Trophy,
      accentClassName: "bg-primary/10 text-primary",
      isLoading: leaderboardQuery.isLoading || profileQuery.isLoading,
      isError: leaderboardQuery.isError || profileQuery.isError,
    },
  ];

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {items.map((item) => (
        <Card
          key={item.label}
          className="border-border bg-card transition-all hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-lg hover:shadow-primary/5"
        >
          <CardContent className="flex items-center gap-3 pt-6">
            <div className={cn("flex h-9 w-9 shrink-0 items-center justify-center rounded-lg", item.accentClassName)}>
              <item.icon className="h-4.5 w-4.5" />
            </div>
            <div>
              <p className="text-xs uppercase tracking-wide text-muted-foreground">{item.label}</p>
              {item.isLoading ? (
                <Skeleton className="mt-1 h-6 w-16" />
              ) : item.isError ? (
                <p className="font-heading text-lg font-bold text-destructive">—</p>
              ) : (
                <p className="font-heading text-xl font-bold">{item.value}</p>
              )}
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
