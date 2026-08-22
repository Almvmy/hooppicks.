import Link from "next/link";
import { Radio, Clock } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { MatchOddsRow } from "@/components/match-odds-row";
import { Match } from "@/lib/types";
import { cn } from "@/lib/utils";

export function PickOfDay({ match }: { match: Match | undefined }) {
  if (!match) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center gap-1 py-10 text-center">
          <p className="font-heading text-base font-bold">Aucun match programmé</p>
          <p className="text-sm text-muted-foreground">
            Reviens plus tard, le calendrier NBA n&apos;est pas encore ouvert.
          </p>
        </CardContent>
      </Card>
    );
  }

  const isLive = match.status === "live";
  const date = new Date(match.date);

  return (
    <Card>
      <CardContent className="pt-6">
        <div className="flex items-center justify-between">
          <span
            className={cn(
              "flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide",
              isLive ? "text-destructive" : "text-primary"
            )}
          >
            {isLive ? (
              <>
                <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-destructive" />
                En direct
              </>
            ) : (
              <>
                <Radio className="h-3.5 w-3.5" />
                Prochain coup d&apos;envoi
              </>
            )}
          </span>
          <span className="flex items-center gap-1 font-mono text-xs text-muted-foreground">
            <Clock className="h-3.5 w-3.5" />
            {date.toLocaleDateString("fr-FR", { day: "2-digit", month: "short" })}
            {" · "}
            {date.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })}
          </span>
        </div>

        <Link
          href={`/matches/${match.id}`}
          className="mt-4 flex items-center justify-between gap-4"
        >
          <div className="flex-1">
            <div className="flex items-baseline justify-between">
              <span className="font-heading text-lg font-bold">
                {match.awayTeam.name}
              </span>
              {match.status !== "scheduled" && (
                <span className="font-mono text-lg font-bold">{match.awayScore}</span>
              )}
            </div>
            <div className="mt-1 flex items-baseline justify-between">
              <span className="font-heading text-lg font-bold">
                {match.homeTeam.name}
              </span>
              {match.status !== "scheduled" && (
                <span className="font-mono text-lg font-bold">{match.homeScore}</span>
              )}
            </div>
          </div>
        </Link>

        {match.status === "scheduled" && (
          <div className="mt-4">
            <MatchOddsRow match={match} />
          </div>
        )}
      </CardContent>
    </Card>
  );
}
