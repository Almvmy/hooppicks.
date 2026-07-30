import Link from "next/link";
import { Card, CardContent } from "@/components/ui/card";
import { MatchStatusBadge } from "@/components/match-status-badge";
import { Match } from "@/lib/types";
import { MatchOddsRow } from "@/components/match-odds-row";

export function MatchCard({ match }: { match: Match }) {
  const date = new Date(match.date);

  return (
    <Link href={`/matches/${match.id}`}>
      <Card className="border-border bg-card transition-colors hover:border-primary/40">
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

