"use client";

import { Radio } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { TeamLogo } from "@/components/team-logo";
import { LiveMatch, liveClockLabel, periodColumnLabel } from "@/lib/live";
import type { Match, PlayerBoxScore, Team } from "@/lib/types";
import { cn } from "@/lib/utils";

function SectionTitle({ children }: { children: React.ReactNode }) {
  return <h2 className="font-heading text-sm font-bold uppercase tracking-wide text-muted-foreground">{children}</h2>;
}

function Linescore({ match, live }: { match: Match; live: LiveMatch }) {
  const columns = Math.max(4, live.homeLinescores.length, live.awayLinescores.length);
  const rows: { team: Team; lines: number[]; total: number | null }[] = [
    { team: match.awayTeam, lines: live.awayLinescores, total: live.status.awayScore },
    { team: match.homeTeam, lines: live.homeLinescores, total: live.status.homeScore },
  ];
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm tabular-nums">
        <thead>
          <tr className="text-[11px] text-muted-foreground">
            <th className="py-1 text-left font-medium">Équipe</th>
            {Array.from({ length: columns }, (_, i) => (
              <th key={i} className="w-9 py-1 text-center font-medium">
                {periodColumnLabel(i)}
              </th>
            ))}
            <th className="w-12 py-1 text-right font-medium">Total</th>
          </tr>
        </thead>
        <tbody>
          {rows.map(({ team, lines, total }) => (
            <tr key={team.id} className="border-t border-tint/10">
              <td className="py-2">
                <span className="flex items-center gap-2 font-semibold">
                  <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={20} />
                  {team.abbreviation}
                </span>
              </td>
              {Array.from({ length: columns }, (_, i) => (
                <td key={i} className="py-2 text-center font-mono text-muted-foreground">
                  {i < lines.length ? lines[i] : "-"}
                </td>
              ))}
              <td className="py-2 text-right font-mono font-bold">{total ?? "-"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

const LEADER_STATS: { label: string; value: (p: PlayerBoxScore) => number }[] = [
  { label: "Points", value: (p) => p.points },
  { label: "Rebonds", value: (p) => p.rebounds },
  { label: "Passes", value: (p) => p.assists },
];

function best(players: PlayerBoxScore[], value: (p: PlayerBoxScore) => number) {
  return players.reduce<PlayerBoxScore | undefined>((top, p) => (!top || value(p) > value(top) ? p : top), undefined);
}

function Leaders({ match, players }: { match: Match; players: PlayerBoxScore[] }) {
  const teams = [match.awayTeam, match.homeTeam];
  return (
    <div className="grid gap-2 sm:grid-cols-3">
      {LEADER_STATS.map((stat) => (
        <div key={stat.label} className="glass-inset-quiet rounded-xl px-3 py-2.5">
          <p className="mb-1.5 text-[11px] uppercase tracking-wide text-muted-foreground">{stat.label}</p>
          {teams.map((team) => {
            const leader = best(
              players.filter((p) => p.teamAbbreviation === team.abbreviation),
              stat.value
            );
            return (
              <p key={team.id} className="flex items-center gap-2 py-0.5 text-sm">
                <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={16} />
                <span className="min-w-0 flex-1 truncate">{leader?.playerName ?? "-"}</span>
                <span className="font-mono font-bold">{leader ? stat.value(leader) : ""}</span>
              </p>
            );
          })}
        </div>
      ))}
    </div>
  );
}

/** Part de la barre pour l'extérieur : « 25-48 » se compare sur les réussis, « 53 » tel quel. */
function share(away: string, home: string): number | null {
  const n = (v: string) => Number.parseFloat(v.split("-")[0]);
  const a = n(away);
  const h = n(home);
  if (!Number.isFinite(a) || !Number.isFinite(h) || a + h === 0) return null;
  return a / (a + h);
}

function TeamStats({ match, live }: { match: Match; live: LiveMatch }) {
  return (
    <div className="flex flex-col gap-2.5">
      <div className="flex items-center justify-between text-xs font-semibold">
        <span className="flex items-center gap-1.5">
          <TeamLogo abbreviation={match.awayTeam.abbreviation} logoUrl={match.awayTeam.logoUrl} size={18} />
          {match.awayTeam.abbreviation}
        </span>
        <span className="flex items-center gap-1.5">
          {match.homeTeam.abbreviation}
          <TeamLogo abbreviation={match.homeTeam.abbreviation} logoUrl={match.homeTeam.logoUrl} size={18} />
        </span>
      </div>
      {live.teamStats.map((stat) => {
        const awayShare = share(stat.away, stat.home);
        return (
          <div key={stat.label}>
            <div className="flex items-center justify-between text-sm tabular-nums">
              <span className={cn("font-mono", awayShare !== null && awayShare > 0.5 && "font-bold")}>{stat.away}</span>
              <span className="text-xs text-muted-foreground">{stat.label}</span>
              <span className={cn("font-mono", awayShare !== null && awayShare < 0.5 && "font-bold")}>{stat.home}</span>
            </div>
            {awayShare !== null && (
              <div className="mt-1 flex h-1.5 overflow-hidden rounded-full bg-tint/10" aria-hidden>
                <span className="bg-primary/80" style={{ width: `${awayShare * 100}%` }} />
                <span className="flex-1 bg-tint/25" />
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

/** Score par quart-temps, avec le chrono tant que le match se joue. */
export function LinescoreSection({ match, live }: { match: Match; live: LiveMatch }) {
  return (
    <Card>
      <CardContent className="flex flex-col gap-4 pt-6">
        <div className="flex items-center justify-between gap-2">
          <SectionTitle>Quarts-temps</SectionTitle>
          {live.status.state === "in" && (
            <span className="flex shrink-0 items-center gap-1.5 whitespace-nowrap font-mono text-xs font-semibold text-live">
              <Radio className="h-3.5 w-3.5 animate-pulse" />
              {liveClockLabel(live.status)}
            </span>
          )}
        </div>
        <Linescore match={match} live={live} />
      </CardContent>
    </Card>
  );
}

export function LeadersSection({ match, players }: { match: Match; players: PlayerBoxScore[] }) {
  if (players.length === 0) return null;
  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-6">
        <SectionTitle>Meilleurs joueurs du match</SectionTitle>
        <Leaders match={match} players={players} />
      </CardContent>
    </Card>
  );
}

export function TeamStatsSection({ match, live }: { match: Match; live: LiveMatch }) {
  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-6">
        <SectionTitle>Stats des équipes</SectionTitle>
        <TeamStats match={match} live={live} />
      </CardContent>
    </Card>
  );
}
