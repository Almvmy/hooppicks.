"use client";

import Link from "next/link";
import { Flame } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { OddsButton } from "@/components/odds-button";
import { TeamLogo } from "@/components/team-logo";
import { useBoost } from "@/lib/boost";
import type { Match } from "@/lib/types";
import { formatMatchDate, formatMatchTime, isBettable } from "@/lib/utils";

/** Le match à la cote boostée de la soirée, avec ses deux cotes « vainqueur » boostées. */
export function BoostCard({ matches }: { matches: Match[] | undefined }) {
  const boost = useBoost();
  const match = boost ? matches?.find((m) => m.id === boost.matchId) : undefined;
  if (!boost || !match || !isBettable(match)) return null;

  const date = new Date(match.date);
  const sides = [
    { team: match.awayTeam, outcome: "away" as const, odds: boost.awayOdds, original: match.odds.moneylineAway },
    { team: match.homeTeam, outcome: "home" as const, odds: boost.homeOdds, original: match.odds.moneylineHome },
  ];

  return (
    <Card className="relative overflow-hidden shadow-[inset_0_0_0_1px_var(--brand),var(--lift)]">
      <CardContent className="flex flex-col gap-3 pt-5">
        <div className="flex items-center justify-between gap-2">
          <span className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wide text-primary">
            <Flame className="h-4 w-4" />
            Cote boostée du jour
          </span>
          <span className="font-mono text-xs text-muted-foreground">
            {formatMatchDate(date)} · {formatMatchTime(date)}
          </span>
        </div>
        <Link href={`/matches/${match.id}`} className="flex items-center justify-center gap-3 font-heading font-bold">
          <TeamLogo abbreviation={match.awayTeam.abbreviation} logoUrl={match.awayTeam.logoUrl} size={32} />
          <span className="truncate">{match.awayTeam.name}</span>
          <span className="text-xs text-muted-foreground">@</span>
          <span className="truncate">{match.homeTeam.name}</span>
          <TeamLogo abbreviation={match.homeTeam.abbreviation} logoUrl={match.homeTeam.logoUrl} size={32} />
        </Link>
        <div className="flex gap-2">
          {sides.map(({ team, outcome, odds, original }) => (
            <OddsButton
              key={outcome}
              boostedFrom={original}
              selection={{
                id: `${match.id}-moneyline-${outcome}`,
                matchId: match.id,
                matchLabel: `${match.awayTeam.name} vs ${match.homeTeam.name}`,
                market: "moneyline",
                outcome,
                label: `${team.abbreviation} (V)`,
                odds,
              }}
            />
          ))}
        </div>
        <p className="text-center text-[11px] text-muted-foreground">
          Vainqueur du match payé +{Math.round((boost.factor - 1) * 100)} % · mise limitée à {boost.maxStake} pts par
          ticket
        </p>
      </CardContent>
    </Card>
  );
}
