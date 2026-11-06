import Link from "next/link";
import { Newspaper } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { NewsItem } from "@/lib/types";
import { formatRelativeTime } from "@/lib/utils";

export function NewsPreview({
  items,
  isLoading,
}: {
  items: NewsItem[];
  isLoading: boolean;
}) {
  const top = items.slice(0, 3);
  // La première actu en grand quand elle a une photo : une image donne envie, trois vignettes non.
  const lead = top[0]?.imageUrl ? top[0] : null;
  const rows = lead ? top.slice(1) : top;

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle className="font-heading text-base">Actualités</CardTitle>
        <Link href="/news" className="text-xs font-medium text-primary hover:underline">
          Tout voir
        </Link>
      </CardHeader>
      <CardContent className="flex flex-col gap-1">
        {isLoading &&
          Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-10 w-full rounded-xl" />)}

        {!isLoading && top.length === 0 && (
          <p className="flex flex-col items-center gap-2 py-4 text-center text-sm text-muted-foreground">
            <Newspaper className="h-6 w-6" />
            Pas d&apos;actualité disponible pour l&apos;instant.
          </p>
        )}

        {!isLoading && lead && (
          <a href={lead.link} target="_blank" rel="noopener noreferrer" className="group relative mb-1 block overflow-hidden rounded-xl">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={lead.imageUrl!} alt="" loading="lazy" className="aspect-video w-full object-cover transition-transform duration-500 group-hover:scale-[1.03]" />
            <span aria-hidden className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/40 to-transparent" />
            <span className="absolute inset-x-0 bottom-0 flex flex-col gap-0.5 p-3 text-white">
              <span className="line-clamp-2 font-heading text-sm font-bold leading-snug">{lead.title}</span>
              <span className="font-mono text-xs text-white/75">{formatRelativeTime(lead.publishedAt)}</span>
            </span>
          </a>
        )}

        {!isLoading &&
          rows.map((item) => (
            <a
              key={item.link}
              href={item.link}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-3 rounded-xl px-2 py-1.5 transition-colors hover:bg-tint/[0.06]"
            >
              {item.imageUrl && (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={item.imageUrl} alt="" loading="lazy" className="h-12 w-16 shrink-0 rounded-lg object-cover" />
              )}
              <span className="flex min-w-0 flex-col gap-0.5">
                <span className="line-clamp-2 text-sm font-medium leading-snug">{item.title}</span>
                <span className="font-mono text-xs text-muted-foreground">
                  {formatRelativeTime(item.publishedAt)}
                </span>
              </span>
            </a>
          ))}
      </CardContent>
    </Card>
  );
}
