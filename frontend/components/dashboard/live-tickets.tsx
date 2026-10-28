"use client";

import Link from "next/link";
import { Radio } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { TeamLogo } from "@/components/team-logo";
import { LEG_STATE_LABELS } from "@/components/my-bet-chip";
import { legState } from "@/lib/bet-legs";
import { liveClockLabel, useLiveStatus, withLiveScore } from "@/lib/live";
import type { BetSelection, Match, PlacedBet } from "@/lib/types";
import { cn } from "@/lib/utils";

type PlacedSelection = PlacedBet["selections"][number];

function LiveRow({ match, selections }: { match: Match; selections: PlacedSelection[] }) {
  const live = useLiveStatus(match);
  const m = withLiveScore(match, live);
  return (
    <Link href={`/matches/${match.id}`} className="flex flex-col gap-1.5 py-3 first:pt-1 last:pb-1">
      <div className="flex items-center gap-2">
        <TeamLogo abbreviation={m.awayTeam.abbreviation} logoUrl={m.awayTeam.logoUrl} size={22} />
        <span className="font-semibold">{m.awayTeam.abbreviation}</span>
        <span className="font-mono text-lg font-bold tabular-nums">
          {m.awayScore ?? 0} – {m.homeScore ?? 0}
        </span>
        <span className="font-semibold">{m.homeTeam.abbreviation}</span>
        <TeamLogo abbreviation={m.homeTeam.abbreviation} logoUrl={m.homeTeam.logoUrl} size={22} />
        <span className="ml-auto shrink-0 text-xs font-semibold text-destructive">{live ? liveClockLabel(live) : "En cours"}</span>
      </div>
      {selections.map((s) => {
        const state = LEG_STATE_LABELS[legState(s as Pick<BetSelection, "market" | "outcome" | "propLine">, m)];
        return (
          <p key={s.id} className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <span className="truncate">{s.label}</span>
            {state && <span className={cn("shrink-0 font-semibold", state.className)}>· {state.label}</span>}
          </p>
        );
      })}
    </Link>
  );
}

/**
 * Matchs en cours sur lesquels on a un ticket : le score et l'état de sa
 * sélection, en haut de l'accueil, sans aller dans Mes paris. Rien s'il n'y
 * en a pas.
 */
export function LiveTickets({ matches, bets }: { matches: Match[] | undefined; bets: PlacedBet[] | undefined }) {
  const pending = (bets ?? []).filter((b) => b.status === "pending").flatMap((b) => b.selections);
  const rows = (matches ?? [])
    .filter((m) => m.status === "live")
    .map((m) => ({ match: m, selections: pending.filter((s) => s.matchId === m.id) }))
    .filter((r) => r.selections.length > 0);
  if (rows.length === 0) return null;

  return (
    <Card>
      <CardContent className="flex flex-col pt-5">
        <h2 className="mb-1 flex items-center gap-2 font-heading text-base font-bold">
          <Radio className="h-4 w-4 animate-pulse text-destructive" />
          Tes matchs en direct
        </h2>
        <div className="flex flex-col divide-y divide-tint/10">
          {rows.map((r) => (
            <LiveRow key={r.match.id} match={r.match} selections={r.selections} />
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
