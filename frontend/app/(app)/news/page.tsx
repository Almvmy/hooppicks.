"use client";

import { useMemo, useState } from "react";
import { ExternalLink, Heart, Newspaper, PlayCircle } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { TeamLogo } from "@/components/team-logo";
import { NbaLogo } from "@/components/nba-logo";
import { fetchNews } from "@/lib/api/news";
import { fetchProfile } from "@/lib/api/auth";
import { cn, formatRelativeTime, getDayLabel } from "@/lib/utils";
import { detectTeamMention } from "@/lib/team-mentions";
import { favoriteTeamAbbreviation, useTeamsByAbbreviation } from "@/lib/use-teams";
import { NewsItem, TeamRank } from "@/lib/types";

/**
 * Équipes d'un article : celles étiquetées par ESPN (API JSON) ; en repli
 * sur le flux RSS, qui n'en donne aucune, la détection par mot-clé du titre.
 */
function teamsOf(item: NewsItem): string[] {
  if (item.teams?.length) return item.teams;
  const mentioned = detectTeamMention(item.title);
  return mentioned ? [mentioned] : [];
}

function groupByDay(items: NewsItem[]): [string, NewsItem[]][] {
  const groups = new Map<string, NewsItem[]>();
  for (const item of items) {
    const label = getDayLabel(new Date(item.publishedAt));
    groups.set(label, [...(groups.get(label) ?? []), item]);
  }
  return [...groups.entries()];
}

function TeamLogos({ abbreviations, teams, size }: { abbreviations: string[]; teams: Map<string, TeamRank>; size: number }) {
  if (abbreviations.length === 0) return null;
  return (
    <span className="flex items-center gap-1">
      {abbreviations.slice(0, 3).map((abbr) => (
        <TeamLogo key={abbr} abbreviation={abbr} logoUrl={teams.get(abbr)?.logoUrl} size={size} />
      ))}
    </span>
  );
}

function ArticleImage({ item, className }: { item: NewsItem; className?: string }) {
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

function VideoBadge() {
  return (
    <span className="flex items-center gap-1 rounded-full bg-black/60 px-2 py-0.5 text-[11px] font-semibold text-white backdrop-blur">
      <PlayCircle className="h-3.5 w-3.5" />
      Vidéo
    </span>
  );
}

/** À la une : grande photo, titre posé dessus sur un voile sombre (lisible dans les deux thèmes). */
function FeaturedArticle({ item, teams }: { item: NewsItem; teams: Map<string, TeamRank> }) {
  return (
    <a
      href={item.link}
      target="_blank"
      rel="noopener noreferrer"
      className="group relative block overflow-hidden rounded-3xl shadow-[var(--lift)]"
    >
      <ArticleImage
        item={item}
        className="aspect-[16/9] w-full transition-transform duration-500 group-hover:scale-[1.03] sm:aspect-[21/9]"
      />
      <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/45 to-transparent" />
      <div className="absolute inset-x-0 top-0 flex items-center justify-between p-4">
        <span className="rounded-full bg-brand px-2.5 py-1 text-[11px] font-bold uppercase tracking-wide text-brand-foreground">
          À la une
        </span>
        {item.video && <VideoBadge />}
      </div>
      <div className="absolute inset-x-0 bottom-0 flex flex-col gap-2 p-5 text-white sm:p-7">
        <div className="flex items-center gap-2 text-xs text-white/75">
          <TeamLogos abbreviations={teamsOf(item)} teams={teams} size={26} />
          <span className="font-mono">{formatRelativeTime(item.publishedAt)}</span>
        </div>
        <h2 className="font-heading text-xl font-bold leading-snug sm:text-3xl">{item.title}</h2>
        {item.description && <p className="line-clamp-2 max-w-3xl text-sm text-white/80">{item.description}</p>}
      </div>
    </a>
  );
}

function ArticleCard({ item, teams }: { item: NewsItem; teams: Map<string, TeamRank> }) {
  return (
    <a href={item.link} target="_blank" rel="noopener noreferrer" className="group block h-full">
      <Card className="h-full gap-0 overflow-hidden pt-0 transition-shadow group-hover:shadow-[inset_0_1px_0_rgba(255,255,255,0.30),inset_0_0_0_1px_rgba(255,122,26,0.30),var(--lift)]">
        <div className="relative overflow-hidden">
          <ArticleImage item={item} className="aspect-video w-full transition-transform duration-500 group-hover:scale-[1.04]" />
          {item.video && (
            <div className="absolute right-2 top-2">
              <VideoBadge />
            </div>
          )}
        </div>
        <CardContent className="flex flex-1 flex-col gap-2 pt-4">
          <div className="flex items-center justify-between gap-2">
            <TeamLogos abbreviations={teamsOf(item)} teams={teams} size={22} />
            <span className="ml-auto font-mono text-xs text-muted-foreground">{formatRelativeTime(item.publishedAt)}</span>
          </div>
          <h3 className="line-clamp-3 font-heading text-base font-bold leading-snug">{item.title}</h3>
          {item.description && <p className="line-clamp-2 text-sm text-muted-foreground">{item.description}</p>}
          <span className="mt-auto flex items-center gap-1 pt-1 text-xs font-medium text-primary">
            {item.video ? "Voir sur ESPN" : "Lire sur ESPN"}
            <ExternalLink className="h-3 w-3" />
          </span>
        </CardContent>
      </Card>
    </a>
  );
}

export default function NewsPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["news"],
    queryFn: fetchNews,
    staleTime: 10 * 60 * 1000,
  });
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const teams = useTeamsByAbbreviation();
  const favorite = favoriteTeamAbbreviation(profile?.favoriteTeam, teams);
  const [teamFilter, setTeamFilter] = useState<string | null>(null);

  // Équipes présentes dans le fil, de la plus citée à la moins citée : le
  // filtre ne propose que des équipes qui ont vraiment des articles.
  const teamsInFeed = useMemo(() => {
    const counts = new Map<string, number>();
    for (const item of data ?? []) for (const abbr of teamsOf(item)) counts.set(abbr, (counts.get(abbr) ?? 0) + 1);
    return [...counts.entries()].sort((a, b) => b[1] - a[1]).map(([abbr]) => abbr);
  }, [data]);

  const filtered = (data ?? []).filter((item) => !teamFilter || teamsOf(item).includes(teamFilter));
  const [featured, ...rest] = filtered;
  const groups = groupByDay(rest);
  const favoriteHasNews = !!favorite && teamsInFeed.includes(favorite);

  const chip = (active: boolean) =>
    cn(
      "flex shrink-0 items-center gap-1.5 rounded-full px-3 py-1.5 text-sm transition-colors",
      active ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
    );

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="flex items-center gap-2.5 font-heading text-2xl font-bold">
          <NbaLogo size={30} />
          Actualités
        </h1>
        <p className="mt-1 text-muted-foreground">Les dernières nouvelles NBA, via ESPN, traduites en français.</p>
      </div>

      {teamsInFeed.length > 0 && (
        <div className="glass-scroll -mx-1 flex gap-2 overflow-x-auto px-1 pb-1" role="group" aria-label="Filtrer par équipe">
          <button type="button" className={chip(teamFilter === null)} onClick={() => setTeamFilter(null)}>
            Toutes
          </button>
          {favoriteHasNews && (
            <button type="button" className={chip(teamFilter === favorite)} onClick={() => setTeamFilter(favorite)}>
              <Heart className="h-3.5 w-3.5" />
              Mon équipe
            </button>
          )}
          {teamsInFeed.map((abbr) => (
            <button
              key={abbr}
              type="button"
              className={chip(teamFilter === abbr)}
              onClick={() => setTeamFilter(teamFilter === abbr ? null : abbr)}
              aria-pressed={teamFilter === abbr}
              title={teams.get(abbr)?.name ?? abbr}
            >
              <TeamLogo abbreviation={abbr} logoUrl={teams.get(abbr)?.logoUrl} size={20} />
              {abbr}
            </button>
          ))}
        </div>
      )}

      {isLoading && (
        <div className="flex flex-col gap-6">
          <Skeleton className="aspect-[21/9] w-full rounded-3xl" />
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-72 w-full rounded-2xl" />
            ))}
          </div>
        </div>
      )}

      {isError && <p className="text-destructive">Impossible de charger les actualités. Réessaie plus tard.</p>}

      {!isLoading && !isError && filtered.length === 0 && (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-10 text-center text-muted-foreground">
            <Newspaper className="h-8 w-8" />
            {teamFilter ? "Aucune actualité pour cette équipe en ce moment." : "Pas d'actualité disponible pour l'instant."}
          </CardContent>
        </Card>
      )}

      {!isLoading && !isError && featured && (
        <div className="flex flex-col gap-8">
          <FeaturedArticle item={featured} teams={teams} />

          {groups.map(([label, items]) => (
            <section key={label} className="flex flex-col gap-3">
              <h2 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{label}</h2>
              <div className="reveal-children grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {items.map((item) => (
                  <ArticleCard key={item.link} item={item} teams={teams} />
                ))}
              </div>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
