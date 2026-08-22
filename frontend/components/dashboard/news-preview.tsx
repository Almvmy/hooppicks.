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

        {!isLoading &&
          top.map((item) => (
            <a
              key={item.link}
              href={item.link}
              target="_blank"
              rel="noopener noreferrer"
              className="flex flex-col gap-0.5 rounded-xl px-2 py-1.5 transition-colors hover:bg-white/[0.06]"
            >
              <span className="line-clamp-2 text-sm font-medium leading-snug">{item.title}</span>
              <span className="font-mono text-xs text-muted-foreground">
                {formatRelativeTime(item.publishedAt)}
              </span>
            </a>
          ))}
      </CardContent>
    </Card>
  );
}
