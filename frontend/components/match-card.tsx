import Link from "next/link";
import { Swords } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { MatchStatusBadge } from "@/components/match-status-badge";
import { Match } from "@/lib/types";
import { MatchOddsRow } from "@/components/match-odds-row";
import { isRivalryMatchup } from "@/lib/rivalries";
import { cn } from "@/lib/utils";

export function MatchCard({ match }: { match: Match }) {
  const date = new Date(match.date);
  const isRivalry = isRivalryMatchup(match.homeTeam.abbreviation, match.awayTeam.abbreviation);

  return (
    <Link href={`/matches/${match.id}`}>
      <Card
        className={cn(
          "relative overflow-hidden border-border bg-card transition-colors hover:border-primary/40",
          isRivalry && "border-primary/30"
        )}
      >
        {isRivalry && (
          <div className="flex items-center gap-1.5 border-b border-primary/20 bg-primary/10 px-4 py-1.5">
            <Swords className="h-3 w-3 text-primary" />
            <span className="font-mono text-[10px] font-bold uppercase tracking-wide text-primary">
              Rivalité historique
            </span>
          </div>
        )}
        <CardContent className="flex items-center justify-between gap-4 pt-6">
          <div className="flex-1">
            <div className="flex items-center justify-between text-sm">
              <span className="font-medium">{match.awayTeam.name}</span>
              {match.status !== "scheduled" && (
                <span className="font-mono font-bold">{match.awayScore}</span>
              )}
            </div>
            <div className="mt-1 flex items-center justify-between text-sm">
              <span className="font-medium">{match.homeTeam.name}</span>
              {match.status !== "scheduled" && (
                <span className="font-mono font-bold">{match.homeScore}</span>
              )}
            </div>
          </div>

          <div className="flex flex-col items-end gap-2">
            <MatchStatusBadge status={match.status} />
            <span className="font-mono text-xs text-muted-foreground">
              {date.toLocaleDateString("fr-FR", { day: "2-digit", month: "short" })}
              {" · "}
              {date.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })}
            </span>
          </div>
        </CardContent>
        <div className="px-6 pb-4">
          {match.status === "scheduled" ? (
            <MatchOddsRow match={match} />
          ) : (
            <p className="rounded-md bg-secondary/30 px-3 py-2 text-center text-xs text-muted-foreground">
              Paris fermés — ce match {match.status === "live" ? "est en cours" : "est terminé"}
            </p>
          )}
        </div>
      </Card>
    </Link>
  );
}
