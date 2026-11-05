"use client";

import { useQuery } from "@tanstack/react-query";
import { Check, Target } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { fetchMissions } from "@/lib/api/missions";
import { cn } from "@/lib/utils";

/** Missions de la semaine (MissionService) : récompenses cosmétiques, jamais de points. */
export function MissionsCard() {
  const { data, isError } = useQuery({ queryKey: ["missions"], queryFn: fetchMissions, staleTime: 60_000 });
  if (isError || !data) return null;
  const done = data.missions.filter((m) => m.done).length;

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-5">
        <div className="flex items-center justify-between gap-3">
          <h2 className="flex items-center gap-2 font-heading text-base font-bold">
            <Target className="h-4 w-4 text-primary" />
            Missions de la semaine
          </h2>
          <span className="font-mono text-sm font-bold">{done}/{data.missions.length}</span>
        </div>

        <ul className="flex flex-col gap-2.5">
          {data.missions.map((m) => (
            <li key={m.key} className="flex items-center gap-3 text-sm">
              <span
                className={cn(
                  "flex h-5 w-5 shrink-0 items-center justify-center rounded-full",
                  m.done ? "bg-success text-background" : "border border-tint/25"
                )}
                aria-label={m.done ? "Réussie" : "À faire"}
              >
                {m.done && <Check className="h-3.5 w-3.5" />}
              </span>
              <span className={cn("min-w-0 flex-1", m.done && "text-muted-foreground line-through")}>{m.label}</span>
              {!m.done && m.target > 1 && (
                <span className="shrink-0 font-mono text-xs text-muted-foreground">
                  {m.progress.toLocaleString("fr-FR")}/{m.target.toLocaleString("fr-FR")}
                </span>
              )}
            </li>
          ))}
        </ul>

        {data.nextRewardAt !== null && (
          <div className="flex flex-col gap-1.5">
            <div className="h-1.5 overflow-hidden rounded-full bg-tint/10" aria-hidden>
              <div className="h-full rounded-full bg-primary" style={{ width: `${Math.min(100, (data.completed / data.nextRewardAt) * 100)}%` }} />
            </div>
            <p className="text-[11px] text-muted-foreground">
              {data.completed}/{data.nextRewardAt} missions : icône de maillot « {data.nextRewardLabel} »
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
