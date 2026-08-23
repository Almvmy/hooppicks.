"use client";

import { ExternalLink, Newspaper } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchNews } from "@/lib/api/news";
import { formatRelativeTime, getDayLabel } from "@/lib/utils";
import { getTeamColor } from "@/lib/team-colors";
import { detectTeamMention } from "@/lib/team-mentions";
import { NewsItem } from "@/lib/types";

function groupByDay(items: NewsItem[]): [string, NewsItem[]][] {
  const groups = new Map<string, NewsItem[]>();
  for (const item of items) {
    const label = getDayLabel(new Date(item.publishedAt));
    const existing = groups.get(label);
    if (existing) {
      existing.push(item);
    } else {
      groups.set(label, [item]);
    }
  }
  return [...groups.entries()];
}

function NewsCard({ item, featured = false }: { item: NewsItem; featured?: boolean }) {
  const team = detectTeamMention(item.title);
  const accentColor = team ? getTeamColor(team) : null;

  return (
    <a href={item.link} target="_blank" rel="noopener noreferrer">
      <Card
        className="relative overflow-hidden transition-shadow hover:shadow-[inset_0_1px_0_rgba(255,255,255,0.30),inset_0_0_0_1px_rgba(255,122,26,0.30),0_18px_44px_rgba(3,7,18,0.55)]"
        size={featured ? "default" : "sm"}
      >
        {accentColor && (
          <span
            aria-hidden
            className="absolute inset-y-0 left-0 w-1"
            style={{ backgroundColor: accentColor }}
          />
        )}
        <CardContent className="flex flex-col gap-2 pl-5">
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-1.5">
              <Badge variant="secondary" className="text-[11px]">
                {item.source}
              </Badge>
              {team && (
                <span
                  className="font-mono text-[10px] font-bold uppercase tracking-wide"
                  style={{ color: accentColor ?? undefined }}
                >
                  {team}
                </span>
              )}
            </div>
            <span className="font-mono text-xs text-muted-foreground">
              {formatRelativeTime(item.publishedAt)}
            </span>
          </div>
          <h2
            className={cnTitle(featured)}
          >
            {item.title}
            <ExternalLink className="mt-1 h-3.5 w-3.5 shrink-0 text-muted-foreground" />
          </h2>
          {item.description && (
            <p className={featured ? "line-clamp-3 text-sm text-muted-foreground" : "line-clamp-2 text-sm text-muted-foreground"}>
              {item.description}
            </p>
          )}
        </CardContent>
      </Card>
    </a>
  );
}

function cnTitle(featured: boolean) {
  return featured
    ? "flex items-start gap-1.5 font-heading text-xl font-bold leading-snug"
    : "flex items-start gap-1.5 font-heading text-base font-bold leading-snug";
}

export default function NewsPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["news"],
    queryFn: fetchNews,
    staleTime: 10 * 60 * 1000,
  });

  const groups = data ? groupByDay(data) : [];
  const [firstItem, ...restOfFirstGroup] = groups[0]?.[1] ?? [];

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-bold">Actualités</h1>
        <p className="mt-1 text-muted-foreground">Les dernières nouvelles NBA, via ESPN.</p>
      </div>

      {isLoading && (
        <div className="flex flex-col gap-6">
          <Skeleton className="h-40 w-full rounded-2xl" />
          <div className="flex flex-col gap-3">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-24 w-full rounded-2xl" />
            ))}
          </div>
        </div>
      )}

      {isError && <p className="text-destructive">Impossible de charger les actualités. Réessaie plus tard.</p>}

      {!isLoading && !isError && data?.length === 0 && (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-10 text-center text-muted-foreground">
            <Newspaper className="h-8 w-8" />
            Pas d&apos;actualité disponible pour l&apos;instant.
          </CardContent>
        </Card>
      )}

      {!isLoading && !isError && firstItem && (
        <div className="flex flex-col gap-6">
          <NewsCard item={firstItem} featured />

          {groups.map(([label, items], groupIndex) => {
            const visibleItems = groupIndex === 0 ? restOfFirstGroup : items;
            if (visibleItems.length === 0) return null;
            return (
              <div key={label} className="flex flex-col gap-3">
                <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{label}</p>
                {visibleItems.map((item) => (
                  <NewsCard key={item.link} item={item} />
                ))}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
