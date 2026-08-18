import Link from "next/link";
import { Trophy } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { fetchPublicLeaderboard, fetchPublicMatches } from "@/lib/api/public";
import { Match } from "@/lib/types";

const RANK_COLORS: Record<number, string> = {
  1: "text-primary",
  2: "text-muted-foreground",
  3: "text-muted-foreground",
};

function nextScheduledMatches(matches: Match[] | undefined, limit: number): Match[] {
  if (!matches) return [];
  const now = Date.now();
  return matches
    .filter((m) => m.status === "scheduled" && new Date(m.date).getTime() >= now)
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
    .slice(0, limit);
}

export default async function HomePage() {
  const [leaderboard, matches] = await Promise.all([
    fetchPublicLeaderboard(),
    fetchPublicMatches(),
  ]);

  const topPlayers = leaderboard?.slice(0, 5) ?? [];
  const upcoming = nextScheduledMatches(matches, 3);

  return (
    <div className="flex min-h-screen flex-col items-center bg-background px-4 py-16">
      <div className="flex w-full max-w-md flex-col items-center text-center">
        <span className="mb-4 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 font-mono text-xs text-primary">
          Saison NBA 2026 — Points virtuels
        </span>
        <h1 className="font-heading text-4xl font-bold tracking-tight sm:text-5xl">
          Hoop<span className="text-primary">Picks</span>
        </h1>
        <p className="mt-4 max-w-md text-muted-foreground">
          Pronostique sur chaque match de la saison NBA, grimpe dans le
          classement, aucun argent réel en jeu.
        </p>
        <div className="mt-8 flex gap-3">
          <Link href="/register" className={cn(buttonVariants({ size: "lg" }))}>
            Créer un compte
          </Link>
          <Link
            href="/login"
            className={cn(buttonVariants({ size: "lg", variant: "outline" }))}
          >
            Se connecter
          </Link>
        </div>
      </div>

      {(topPlayers.length > 0 || upcoming.length > 0) && (
        <div className="mt-16 grid w-full max-w-4xl gap-6 sm:grid-cols-2">
          {topPlayers.length > 0 && (
            <div className="rounded-xl border border-border bg-card p-5 text-left">
              <h2 className="font-heading text-sm font-bold uppercase tracking-wide text-muted-foreground">
                Classement de la semaine
              </h2>
              <div className="mt-4 flex flex-col gap-2">
                {topPlayers.map((entry) => (
                  <div key={entry.rank} className="flex items-center justify-between text-sm">
                    <div className="flex items-center gap-2">
                      {entry.rank <= 3 ? (
                        <Trophy className={cn("h-3.5 w-3.5", RANK_COLORS[entry.rank])} />
                      ) : (
                        <span className="w-3.5" />
                      )}
                      <span className={cn("font-mono text-xs font-bold", RANK_COLORS[entry.rank])}>
                        #{entry.rank}
                      </span>
                      <span className="truncate">{entry.username}</span>
                    </div>
                    <span className="font-mono text-xs font-bold">
                      {entry.points.toLocaleString("fr-FR")} pts
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {upcoming.length > 0 && (
            <div className="rounded-xl border border-border bg-card p-5 text-left">
              <h2 className="font-heading text-sm font-bold uppercase tracking-wide text-muted-foreground">
                Prochains matchs
              </h2>
              <div className="mt-4 flex flex-col gap-2">
                {upcoming.map((match) => {
                  const date = new Date(match.date);
                  return (
                    <div key={match.id} className="flex items-center justify-between text-sm">
                      <span className="truncate font-medium">
                        {match.awayTeam.abbreviation}
                        <span className="mx-1 text-muted-foreground">@</span>
                        {match.homeTeam.abbreviation}
                      </span>
                      <span className="shrink-0 font-mono text-xs text-muted-foreground">
                        {date.toLocaleDateString("fr-FR", { day: "2-digit", month: "short" })}
                        {" · "}
                        {date.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
