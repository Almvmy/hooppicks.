import Link from "next/link";
import { ChevronRight, PlayCircle } from "lucide-react";
import { TeamLogo } from "@/components/team-logo";
import { NbaLogo } from "@/components/nba-logo";
import { Match, NewsItem, TeamRank } from "@/lib/types";
import { cn, formatMatchDate, formatMatchTime } from "@/lib/utils";

export function TeamLogos({ abbreviations, teams, size }: { abbreviations: string[]; teams: Map<string, TeamRank>; size: number }) {
  if (abbreviations.length === 0) return null;
  return (
    <span className="flex items-center gap-1">
      {abbreviations.slice(0, 3).map((abbr) => (
        <TeamLogo key={abbr} abbreviation={abbr} logoUrl={teams.get(abbr)?.logoUrl} size={size} />
      ))}
    </span>
  );
}

export function ArticleImage({ item, className }: { item: NewsItem; className?: string }) {
  if (!item.imageUrl) {
    // Pas de photo (repli RSS) : un aplat discret plutôt qu'un trou.
    return (
      <div className={cn("flex items-center justify-center bg-tint/[0.05]", className)}>
        <NbaLogo size={40} className="opacity-40" />
      </div>
    );
  }
  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img src={item.imageUrl} alt="" loading="lazy" className={cn("object-cover", className)} />
  );
}

export function VideoBadge() {
  return (
    <span className="flex items-center gap-1 rounded-full bg-black/60 px-2 py-0.5 text-[11px] font-semibold text-white backdrop-blur">
      <PlayCircle className="h-3.5 w-3.5" />
      Vidéo
    </span>
  );
}

export function NewBadge({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        "rounded-full bg-brand px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-brand-foreground",
        className
      )}
    >
      Nouveau
    </span>
  );
}

/**
 * Raccourci vers le prochain match d'une équipe de l'article, pour parier
 * dessus. Sur deux niveaux (libellé au-dessus des équipes, date empilée à
 * droite) : sur une seule ligne, tout se chevauchait dans une carte étroite.
 */
export function NextMatchLink({ match, className }: { match: Match; className?: string }) {
  const date = new Date(match.date);
  return (
    <Link
      href={`/matches/${match.id}`}
      className={cn(
        "glass-inset-quiet flex items-center gap-3 rounded-xl px-3 py-2 text-xs transition-colors hover:text-foreground",
        className
      )}
    >
      <span className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className="text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">Prochain match</span>
        <span className="flex items-center gap-1 font-mono font-bold">
          <TeamLogo abbreviation={match.awayTeam.abbreviation} logoUrl={match.awayTeam.logoUrl} size={16} />
          {match.awayTeam.abbreviation}
          <span className="text-muted-foreground">@</span>
          <TeamLogo abbreviation={match.homeTeam.abbreviation} logoUrl={match.homeTeam.logoUrl} size={16} />
          {match.homeTeam.abbreviation}
        </span>
      </span>
      <span className="flex shrink-0 flex-col items-end font-mono text-muted-foreground">
        <span>{formatMatchDate(date)}</span>
        <span className="font-bold text-foreground">{formatMatchTime(date)}</span>
      </span>
      <ChevronRight className="h-3.5 w-3.5 shrink-0 text-primary" />
    </Link>
  );
}
