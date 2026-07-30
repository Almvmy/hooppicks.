"use client";

import { useQuery } from "@tanstack/react-query";
import { Award, Lock } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchBadges } from "@/lib/mock/badges";
import { cn } from "@/lib/utils";

export function BadgeGrid() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["badges"],
    queryFn: fetchBadges,
  });

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
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
      {data?.map((badge) => (
        <div
          key={badge.id}
          className={cn(
            "flex flex-col items-center gap-2 rounded-lg border p-4 text-center transition-colors",
            badge.unlocked
              ? "border-primary/30 bg-primary/5"
              : "border-border bg-secondary/30 opacity-50"
          )}
        >
          {badge.unlocked ? (
            <Award className="h-6 w-6 text-primary" />
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
      ))}
    </div>
  );
}