"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ChevronRight, Medal } from "lucide-react";
import { fetchSeasonPicks } from "@/lib/api/season-picks";
import { formatMatchDate } from "@/lib/utils";

/**
 * Rappel des pronostics de saison, seulement tant qu'ils sont ouverts et
 * incomplets : une fois verrouillés ou remplis, il disparaît du tableau de bord.
 */
export function SeasonPicksNudge() {
  const { data } = useQuery({ queryKey: ["season-picks"], queryFn: fetchSeasonPicks, staleTime: 5 * 60 * 1000 });
  if (!data || data.locked) return null;
  const done = Object.keys(data.myPicks).length;
  if (done >= data.questions.length) return null;

  return (
    <Link href="/saison" className="glass-accent flex items-center gap-3 rounded-2xl px-4 py-3 transition-transform hover:-translate-y-0.5">
      <Medal className="h-5 w-5 shrink-0 text-primary" />
      <div className="min-w-0 flex-1">
        <p className="font-semibold">Pronostics de saison : {done}/{data.questions.length}</p>
        <p className="truncate text-xs text-muted-foreground">
          Champion, finalistes, meilleur bilan
          {data.deadline ? ` · avant le ${formatMatchDate(new Date(data.deadline))}` : ""}
        </p>
      </div>
      <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" />
    </Link>
  );
}
