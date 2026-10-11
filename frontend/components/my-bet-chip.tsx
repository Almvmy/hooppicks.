"use client";

import { useQuery } from "@tanstack/react-query";
import { Ticket } from "lucide-react";
import { fetchBets } from "@/lib/api/bets";
import { LegState, legState } from "@/lib/bet-legs";
import type { Match } from "@/lib/types";
import { cn } from "@/lib/utils";

const STATE: Partial<Record<LegState, { label: string; className: string }>> = {
  winning: { label: "bien parti", className: "text-success" },
  losing: { label: "mal parti", className: "text-destructive" },
  level: { label: "à égalité", className: "text-muted-foreground" },
  won: { label: "gagné", className: "text-success" },
  lost: { label: "perdu", className: "text-destructive" },
  push: { label: "remboursé", className: "text-muted-foreground" },
};

/**
 * « LAL (V) · bien parti », discret, sur la carte et la page d'un match :
 * on suit son ticket là où on suit le score. Tickets en attente seulement
 * (un ticket réglé se consulte dans Mes paris). `match` porte déjà le score
 * du direct quand il y en a un.
 */
export function MyBetChip({ match, className }: { match: Match; className?: string }) {
  const { data: bets } = useQuery({ queryKey: ["bets"], queryFn: fetchBets, staleTime: 60 * 1000 });
  const mine = (bets ?? [])
    .filter((b) => b.status === "pending")
    .flatMap((b) => b.selections.filter((s) => s.matchId === match.id));
  if (mine.length === 0) return null;

  const first = mine[0];
  const state = STATE[legState(first, match)];
  return (
    <p
      className={cn(
        "flex items-center justify-center gap-1 text-[11px] font-medium text-muted-foreground",
        className
      )}
      title="Ton pari en cours"
    >
      <Ticket className="h-3 w-3 shrink-0 text-primary" aria-label="Ton pari" />
      <span className="truncate">{first.label}</span>
      {state && <span className={cn("shrink-0", state.className)}>· {state.label}</span>}
      {mine.length > 1 && <span className="shrink-0 text-muted-foreground">+{mine.length - 1}</span>}
    </p>
  );
}
