import Link from "next/link";
import { Swords } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { MatchStatusBadge } from "@/components/match-status-badge";
import { Match } from "@/lib/types";
import { MatchOddsRow } from "@/components/match-odds-row";
import { FaceOffTeams, TeamWatermarks, faceOffBackground } from "@/components/match-face-off";
import { NbaLogo } from "@/components/nba-logo";
import { isRivalryMatchup } from "@/lib/rivalries";
import { cn, formatKickoffCountdown, formatMatchDate, formatMatchTime } from "@/lib/utils";

export function MatchCard({ match }: { match: Match }) {
  const date = new Date(match.date);
  const isRivalry = isRivalryMatchup(match.homeTeam.abbreviation, match.awayTeam.abbreviation);
  const countdown = match.status === "scheduled" ? formatKickoffCountdown(match.date) : null;

  return (
    <Link href={`/matches/${match.id}`}>
      <Card
        style={faceOffBackground(match)}
        className={cn(
          // Le hover éclaircit le verre au lieu de changer la bordure.
          "relative overflow-hidden transition-shadow hover:shadow-[inset_0_1px_0_rgba(255,255,255,0.30),inset_0_0_0_1px_rgba(255,122,26,0.30),var(--lift)]",
          isRivalry && "shadow-[inset_0_1px_0_rgba(255,255,255,0.22),inset_0_0_0_1px_rgba(255,122,26,0.35),var(--lift)]"
        )}
      >
        <TeamWatermarks match={match} />

        {isRivalry && (
          <div className="relative flex items-center gap-1.5 bg-primary/[0.12] px-4 py-1.5 shadow-[inset_0_-1px_0_rgba(255,122,26,0.25)]">
            <Swords className="h-3 w-3 text-primary" />
            <span className="font-mono text-[10px] font-bold tracking-wide uppercase text-[var(--primary-lit)]">
              Rivalité historique
            </span>
          </div>
        )}

        <CardContent className="relative pt-5">
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <NbaLogo size={18} />
              <MatchStatusBadge status={match.status} />
            </div>
            <div className="flex flex-col items-end gap-1">
              <span className="font-mono text-xs text-muted-foreground">
                {formatMatchDate(date)}
                {" · "}
                {formatMatchTime(date)}
              </span>
              {countdown && (
                <span className="rounded-full bg-amber-500/15 px-2 py-0.5 font-mono text-[10px] font-bold text-amber-500 light:text-amber-800">
                  {countdown}
                </span>
              )}
            </div>
          </div>

          <div className="mt-3">
            <FaceOffTeams match={match} morph />
          </div>
        </CardContent>

        <div className="relative px-6 pb-4">
          {match.status === "scheduled" ? (
            <MatchOddsRow match={match} />
          ) : (
            <p className="glass-inset-quiet mt-3 rounded-xl px-3 py-2 text-center text-xs text-muted-foreground">
              paris fermés : match {match.status === "live" ? "en cours" : "terminé"}
            </p>
          )}
        </div>
      </Card>
    </Link>
  );
}
