"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { Lock } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchBadges } from "@/lib/api/badges"
import { badgeIcon } from "@/lib/badges";
import { cn } from "@/lib/utils";

export function BadgeGrid() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["badges"],
    queryFn: fetchBadges,
  });

  // Débloqués d'abord : ce qu'on a accompli mérite d'être vu avant ce qui reste à faire.
  const sorted = useMemo(
    () => (data ? [...data].sort((a, b) => Number(b.unlocked) - Number(a.unlocked)) : undefined),
    [data]
  );
  const unlockedCount = data?.filter((b) => b.unlocked).length ?? 0;

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-24 rounded-lg" />
        ))}
      </div>
    );
  }

  if (isError) {
    return <p className="text-destructive">Impossible de charger les badges.</p>;
  }

  return (
    <div className="flex flex-col gap-3">
      {data && (
        <p className="text-xs font-medium text-muted-foreground">
          {unlockedCount}/{data.length} débloqués
        </p>
      )}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        {sorted?.map((badge) => {
          const Icon = badgeIcon(badge.icon);
          return (
            <div
              key={badge.id}
              className={cn(
                "relative flex flex-col items-center gap-2 overflow-hidden rounded-2xl p-4 text-center transition-transform",
                badge.unlocked
                  ? "badge-holo glass-accent hover:-translate-y-0.5"
                  : "glass-inset-quiet opacity-55"
              )}
            >
              {badge.unlocked ? (
                <Icon className="h-6 w-6 text-primary" />
              ) : (
                <Lock className="h-6 w-6 text-muted-foreground" />
              )}
              <div>
                <p className="text-sm font-medium">{badge.label}</p>
                <p className="mt-0.5 text-xs text-muted-foreground">
                  {badge.description}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
