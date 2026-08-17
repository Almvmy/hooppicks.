"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Award, ArrowRight, Lock } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchBadges } from "@/lib/mock/badges";

export function BadgesTeaserCard() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["badges-dashboard"],
    queryFn: fetchBadges,
  });

  const unlocked = data?.filter((b) => b.unlocked).length ?? 0;
  const total = data?.length ?? 0;
  const progress = total > 0 ? (unlocked / total) * 100 : 0;
  const nextBadge = data?.find((b) => !b.unlocked);

  return (
    <Card className="border-border bg-card">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="flex items-center gap-2 font-heading text-lg">
          <Award className="h-4 w-4 text-primary" />
          Badges
        </CardTitle>
        <Link
          href="/profile"
          className="flex items-center gap-1 text-xs font-medium text-muted-foreground transition-colors hover:text-primary"
        >
          Voir le profil
          <ArrowRight className="h-3 w-3" />
        </Link>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {isLoading ? (
          <>
            <Skeleton className="h-2 w-full rounded-full" />
            <Skeleton className="h-10 w-full rounded-lg" />
          </>
        ) : isError ? (
          <p className="text-sm text-destructive">Impossible de charger les badges.</p>
        ) : (
          <>
            <div>
              <div className="flex items-center justify-between text-xs text-muted-foreground">
                <span>
                  {unlocked} / {total} débloqués
                </span>
                <span>{Math.round(progress)}%</span>
              </div>
              <div className="mt-1.5 h-2 w-full overflow-hidden rounded-full bg-secondary">
                <div
                  className="h-full rounded-full bg-primary transition-all"
                  style={{ width: `${progress}%` }}
                />
              </div>
            </div>

            {nextBadge && (
              <div className="flex items-center gap-3 rounded-lg border border-border/60 bg-secondary/20 px-3 py-2.5">
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-secondary text-muted-foreground">
                  <Lock className="h-4 w-4" />
                </div>
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{nextBadge.label}</p>
                  <p className="truncate text-xs text-muted-foreground">{nextBadge.description}</p>
                </div>
              </div>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
}
