"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Flame, Ticket } from "lucide-react";
import { TeamLogo } from "@/components/team-logo";
import { fetchBets } from "@/lib/api/bets";
import { useBoost } from "@/lib/boost";
import { liveClockLabel, useLiveStatus, withLiveScore } from "@/lib/live";
import type { Match, Team } from "@/lib/types";
import { cn, formatMatchTime } from "@/lib/utils";

interface TeamLineProps {
  team: Team;
  score: number | null | undefined;
  /** En tête (pendant le match) ou vainqueur (après). */
  ahead: boolean;
  showScore: boolean;
}

function TeamLine({ team, score, ahead, showScore }: TeamLineProps) {
  return (
    <span className="flex items-center gap-2">
      <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={20} />
      <span className={cn("min-w-0 flex-1 truncate", ahead ? "font-semibold" : showScore && "text-muted-foreground")}>
        {team.name}
      </span>
      {showScore && (
        <span className={cn("font-mono tabular-nums", ahead ? "font-bold" : "text-muted-foreground")}>{score ?? 0}</span>
      )}
    </span>
  );
}

/**
 * Un match en une ligne, façon appli de scores : heure (ou chrono en direct)
 * à gauche, les deux équipes l'une sous l'autre avec leur score. Pour suivre
 * une soirée chargée sur téléphone ; on parie depuis la page du match.
 */
export function CompactMatchRow({ match: synced }: { match: Match }) {
  const live = useLiveStatus(synced);
  const match = withLiveScore(synced, live);
  const boost = useBoost();
  const { data: bets } = useQuery({ queryKey: ["bets"], queryFn: fetchBets, staleTime: 60 * 1000 });
  const hasBet = (bets ?? []).some((b) => b.status === "pending" && b.selections.some((s) => s.matchId === match.id));

  const started = match.status !== "scheduled";
  const away = match.awayScore ?? 0;
  const home = match.homeScore ?? 0;
  const finished = match.status === "finished";

  return (
    <Link
      href={`/matches/${match.id}`}
      className="glass-inset-quiet flex items-center gap-3 rounded-xl px-3 py-2 text-sm transition-colors hover:bg-tint/[0.06]"
    >
      <span
        className={cn(
          "w-14 shrink-0 text-center font-mono text-[11px] leading-tight",
          match.status === "live" ? "font-semibold text-live" : "text-muted-foreground"
        )}
      >
        {match.status === "live" ? (live ? liveClockLabel(live) : "En direct") : finished ? "Final" : formatMatchTime(new Date(match.date))}
      </span>
      <span className="flex min-w-0 flex-1 flex-col gap-1">
        <TeamLine team={match.awayTeam} score={match.awayScore} ahead={started && away > home} showScore={started} />
        <TeamLine team={match.homeTeam} score={match.homeScore} ahead={started && home > away} showScore={started} />
      </span>
      <span className="flex w-4 shrink-0 flex-col items-center gap-1">
        {boost?.matchId === match.id && <Flame className="h-3.5 w-3.5 text-primary" aria-label="Cote boostée" />}
        {hasBet && <Ticket className="h-3.5 w-3.5 text-primary" aria-label="Tu as un pari sur ce match" />}
      </span>
    </Link>
  );
}
