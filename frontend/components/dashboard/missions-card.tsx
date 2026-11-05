"use client";

import { Check } from "lucide-react";
import type { WeeklyMissions } from "@/lib/api/missions";
import { cn } from "@/lib/utils";

/** Le détail des missions de la semaine (MissionService) : récompenses cosmétiques, jamais de points. */
export function MissionsDetail({ data }: { data: WeeklyMissions }) {
  return (
    <div className="flex flex-col gap-3">
      <ul className="flex flex-col gap-2.5">
        {data.missions.map((m) => (
          <li key={m.key} className="flex items-center gap-3 text-sm">
            <span
              className={cn("flex h-5 w-5 shrink-0 items-center justify-center rounded-full", m.done ? "bg-success text-background" : "border border-tint/25")}
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
    </div>
  );
}
