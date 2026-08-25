import Link from "next/link";
import { Swords } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { MatchStatusBadge } from "@/components/match-status-badge";
import { Match } from "@/lib/types";
import { MatchOddsRow } from "@/components/match-odds-row";
import { isRivalryMatchup } from "@/lib/rivalries";
import { getTeamColor } from "@/lib/team-colors";
import { cn, formatMatchDate, formatMatchTime } from "@/lib/utils";

export function MatchCard({ match }: { match: Match }) {
  const date = new Date(match.date);
  const isRivalry = isRivalryMatchup(match.homeTeam.abbreviation, match.awayTeam.abbreviation);

  return (
    <Link href={`/matches/${match.id}`}>
      <Card
        className={cn(
          // border-* retirés : le Card porte maintenant son liseré via .glass.
          // Le hover éclaircit le verre au lieu de changer la bordure.
          "relative overflow-hidden transition-shadow hover:shadow-[inset_0_1px_0_rgba(255,255,255,0.30),inset_0_0_0_1px_rgba(255,122,26,0.30),0_18px_44px_rgba(3,7,18,0.55)]",
          isRivalry && "shadow-[inset_0_1px_0_rgba(255,255,255,0.22),inset_0_0_0_1px_rgba(255,122,26,0.35),0_18px_44px_rgba(3,7,18,0.55)]"
        )}
      >
        {isRivalry && (
          <div className="flex items-center gap-1.5 bg-primary/[0.12] px-4 py-1.5 shadow-[inset_0_-1px_0_rgba(255,122,26,0.25)]">
            <Swords className="h-3 w-3 text-primary" />
            <span className="font-mono text-[10px] font-bold tracking-wide uppercase text-[var(--primary-lit)]">
              Rivalité historique
            </span>
          </div>
        )}
        <CardContent className="flex items-center justify-between gap-4 pt-6">
          <div className="min-w-0 flex-1">
            <div className="flex items-center justify-between text-sm">
              <span className="flex min-w-0 items-center gap-2 font-medium">
                <span
                  aria-hidden
                  className="h-2.5 w-2.5 shrink-0 rounded-full"
                  style={{ backgroundColor: getTeamColor(match.awayTeam.abbreviation) }}
                />
                <span className="truncate">{match.awayTeam.name}</span>
              </span>
              {match.status !== "scheduled" && (
                <span className="font-mono font-bold">{match.awayScore}</span>
              )}
            </div>
            <div className="mt-1 flex items-center justify-between text-sm">
              <span className="flex min-w-0 items-center gap-2 font-medium">
                <span
                  aria-hidden
                  className="h-2.5 w-2.5 shrink-0 rounded-full"
                  style={{ backgroundColor: getTeamColor(match.homeTeam.abbreviation) }}
                />
                <span className="truncate">{match.homeTeam.name}</span>
              </span>
              {match.status !== "scheduled" && (
                <span className="font-mono font-bold">{match.homeScore}</span>
              )}
            </div>
          </div>

          <div className="flex flex-col items-end gap-2">
            <MatchStatusBadge status={match.status} />
            <span className="font-mono text-xs text-muted-foreground">
              {formatMatchDate(date)}
              {" · "}
              {formatMatchTime(date)}
            </span>
          </div>
        </CardContent>
        <div className="px-6 pb-4">
          {match.status === "scheduled" ? (
            <MatchOddsRow match={match} />
          ) : (
            // bg-secondary/30 → glass-inset-quiet
            <p className="glass-inset-quiet rounded-xl px-3 py-2 text-center text-xs text-muted-foreground">
              paris fermés : match {match.status === "live" ? "en cours" : "terminé"}
            </p>
          )}
        </div>
      </Card>
    </Link>
  );
}
