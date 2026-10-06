"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ChevronRight, Newspaper } from "lucide-react";
import { ArticleImage, TeamLogos, VideoBadge } from "@/components/news/news-parts";
import { fetchNews } from "@/lib/api/news";
import { teamsOf } from "@/lib/news";
import { useTeamsByAbbreviation } from "@/lib/use-teams";
import { formatRelativeTime } from "@/lib/utils";

/**
 * Dernières actus d'une ou deux équipes, là où on décide d'un pari (fiche
 * match, page équipe) : une blessure ou un transfert de la veille change
 * tout. Absent s'il n'y a rien : pas de bloc vide sur chaque fiche.
 */
export function TeamNews({ abbreviations, limit = 4 }: { abbreviations: string[]; limit?: number }) {
  // Même clé que la page Actualités et le tableau de bord : un seul appel.
  const { data, isError } = useQuery({ queryKey: ["news"], queryFn: fetchNews, staleTime: 10 * 60 * 1000 });
  const teams = useTeamsByAbbreviation();

  if (isError) {
    return <p className="text-sm text-muted-foreground">Actualités indisponibles pour le moment.</p>;
  }
  const articles = (data ?? []).filter((item) => teamsOf(item).some((t) => abbreviations.includes(t))).slice(0, limit);
  if (articles.length === 0) return null;

  const allHref = abbreviations.length === 1 ? `/news?equipe=${abbreviations[0]}` : "/news";

  return (
    <section className="flex flex-col gap-3">
      <div className="flex items-center justify-between gap-2">
        <h2 className="flex items-center gap-2 font-heading text-base font-bold">
          <Newspaper className="h-4 w-4 text-primary" />
          Dernières actus
        </h2>
        <Link href={allHref} className="flex items-center gap-0.5 text-sm font-medium text-primary hover:underline">
          Toutes les actus
          <ChevronRight className="h-4 w-4" />
        </Link>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        {articles.map((item) => (
          <a
            key={item.link}
            href={item.link}
            target="_blank"
            rel="noopener noreferrer"
            className="glass group flex gap-3 overflow-hidden rounded-2xl p-2.5 transition-colors hover:bg-tint/[0.04]"
          >
            <div className="relative shrink-0 overflow-hidden rounded-xl">
              <ArticleImage item={item} className="h-20 w-28 transition-transform duration-500 group-hover:scale-[1.05]" />
              {item.video && (
                <div className="absolute bottom-1 left-1 scale-90">
                  <VideoBadge />
                </div>
              )}
            </div>
            <div className="flex min-w-0 flex-col gap-1">
              <h3 className="line-clamp-2 text-sm font-semibold leading-snug">{item.title}</h3>
              <span className="mt-auto flex items-center gap-2 text-xs text-muted-foreground">
                <TeamLogos abbreviations={teamsOf(item)} teams={teams} size={16} />
                <span className="font-mono">{formatRelativeTime(item.publishedAt)}</span>
              </span>
            </div>
          </a>
        ))}
      </div>
    </section>
  );
}
