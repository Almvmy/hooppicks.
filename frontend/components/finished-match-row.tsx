import Link from "next/link";
import { TeamLogo } from "@/components/team-logo";
import { Match } from "@/lib/types";
import { cn } from "@/lib/utils";

/**
 * Match terminé en une ligne : plus rien à parier, seul le score compte. En
 * carte pleine, il prenait autant de place qu'un match ouvert et repoussait
 * ceux-ci en bas de la journée.
 */
export function FinishedMatchRow({ match }: { match: Match }) {
  const away = match.awayScore ?? 0;
  const home = match.homeScore ?? 0;
  const side = (abbr: string, logo: string | null | undefined, score: number, won: boolean, reverse = false) => (
    <span className={cn("flex min-w-0 flex-1 items-center gap-2", reverse && "flex-row-reverse")}>
      <TeamLogo abbreviation={abbr} logoUrl={logo} size={24} />
      <span className={cn("font-semibold", !won && "text-muted-foreground")}>{abbr}</span>
      <span className={cn("ml-auto font-mono text-base", reverse && "ml-0 mr-auto", won ? "font-bold" : "text-muted-foreground")}>
        {score}
      </span>
    </span>
  );
  return (
    <Link
      href={`/matches/${match.id}`}
      className="glass-inset-quiet flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition-colors hover:bg-tint/[0.06]"
    >
      {side(match.awayTeam.abbreviation, match.awayTeam.logoUrl, away, away > home)}
      <span className="shrink-0 text-[10px] uppercase tracking-wide text-muted-foreground">final</span>
      {side(match.homeTeam.abbreviation, match.homeTeam.logoUrl, home, home > away, true)}
    </Link>
  );
}
