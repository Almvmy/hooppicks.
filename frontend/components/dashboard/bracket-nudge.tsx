"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ChevronRight, GitFork } from "lucide-react";
import { fetchBracket } from "@/lib/api/bracket";
import { formatShortDateTime } from "@/lib/utils";

/** Seulement tant que le tableau est ouvert et pas encore rempli. */
export function BracketNudge() {
  const { data } = useQuery({ queryKey: ["bracket"], queryFn: fetchBracket, staleTime: 5 * 60 * 1000 });
  if (!data || data.locked || data.myPicks) return null;

  return (
    <Link href="/coupe" className="glass-accent flex items-center gap-3 rounded-2xl px-4 py-3 transition-transform hover:-translate-y-0.5">
      <GitFork className="h-5 w-5 shrink-0 text-primary" />
      <div className="min-w-0 flex-1">
        <p className="font-semibold">{data.label} : remplis ton tableau</p>
        <p className="truncate text-xs text-muted-foreground">Avant le {formatShortDateTime(new Date(data.locksAt))}</p>
      </div>
      <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" />
    </Link>
  );
}
