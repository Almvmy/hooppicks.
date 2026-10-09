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
 * « Ton pari : LAL (V) · bien parti », sur la carte et la page d'un match :
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
        "flex items-center justify-center gap-1.5 rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold",
        className
      )}
    >
      <Ticket className="h-3.5 w-3.5 shrink-0 text-primary" />
      <span className="truncate">Ton pari : {first.label}</span>
      {state && <span className={cn("shrink-0", state.className)}>· {state.label}</span>}
      {mine.length > 1 && <span className="shrink-0 text-muted-foreground">+{mine.length - 1}</span>}
    </p>
  );
}
