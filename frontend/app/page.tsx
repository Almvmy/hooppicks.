import Image from "next/image";
import Link from "next/link";
import { Trophy } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { cn, formatMatchDate, formatMatchTime } from "@/lib/utils";
import { fetchPublicLeaderboard, fetchPublicMatches } from "@/lib/api/public";
import { Match } from "@/lib/types";
import { LogoSymbol } from "@/app/LogoSymbol";

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
    <div className="relative flex min-h-screen flex-col items-center overflow-hidden bg-background px-4 py-16">
      <Image
        src="/images/auth-court-lines.jpg"
        alt=""
        fill
        preload
        sizes="100vw"
        className="object-cover opacity-60 [filter:brightness(1.6)_contrast(1.15)]"
      />
      <div className="absolute inset-0 bg-gradient-to-b from-background/40 via-background/55 to-background" />

      <div className="relative z-10 flex w-full max-w-md flex-col items-center text-center">
        <span className="mb-4 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 font-mono text-xs text-primary">
          Saison NBA 2026 - Points virtuels
        </span>
        <h1 className="flex items-center gap-3 font-heading text-4xl font-bold tracking-tight sm:text-5xl">
          <LogoSymbol className="h-10 w-10 shrink-0 sm:h-12 sm:w-12" />
          <span>
            Hoop<span className="text-primary">Picks</span>
          </span>
        </h1>
        <p className="mt-4 max-w-md text-muted-foreground">
          Pronostique sur chaque match de la saison NBA, gagne des points et grimpe dans le
          classement.
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
        <div className="relative z-10 mt-16 grid w-full max-w-4xl gap-6 sm:grid-cols-2">
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
                        {formatMatchDate(date)}
                        {" · "}
                        {formatMatchTime(date)}
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
