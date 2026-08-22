import Link from "next/link";
import { ChevronRight } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Match } from "@/lib/types";

export function UpcomingMatches({
  matches,
  isLoading,
}: {
  matches: Match[];
  isLoading: boolean;
}) {
  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle className="font-heading text-base">À venir cette semaine</CardTitle>
        <Link
          href="/matches"
          className="text-xs font-medium text-primary hover:underline"
        >
          Tout voir
        </Link>
      </CardHeader>
      <CardContent className="flex flex-col gap-1">
        {isLoading &&
          Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full rounded-md" />
          ))}

        {!isLoading && matches.length === 0 && (
          <p className="py-4 text-center text-sm text-muted-foreground">
            Pas d&apos;autre match à l&apos;horizon pour l&apos;instant.
          </p>
        )}

        {!isLoading &&
          matches.map((match) => {
            const date = new Date(match.date);
            return (
              <Link
                key={match.id}
                href={`/matches/${match.id}`}
                className="flex items-center justify-between gap-3 rounded-xl px-2 py-2.5 text-sm transition-colors hover:bg-white/[0.06]"
              >
                <div className="flex min-w-0 items-center gap-2">
                  <span className="font-mono text-xs text-muted-foreground">
                    {date.toLocaleDateString("fr-FR", { day: "2-digit", month: "short" })}
                  </span>
                  <span className="truncate font-medium">
                    {match.awayTeam.abbreviation}
                    <span className="mx-1 text-muted-foreground">@</span>
                    {match.homeTeam.abbreviation}
                  </span>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <span className="font-mono text-xs text-muted-foreground">
                    {date.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })}
                  </span>
                  <ChevronRight className="h-4 w-4 text-muted-foreground" />
                </div>
              </Link>
            );
          })}
      </CardContent>
    </Card>
  );
}
