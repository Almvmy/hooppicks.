import Link from "next/link";
import { Shield, Users } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { League } from "@/lib/types";

export function LeaguesPreview({
  leagues,
  isLoading,
}: {
  leagues: League[];
  isLoading: boolean;
}) {
  const top = leagues.slice(0, 3);

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle className="font-heading text-base">Ligues</CardTitle>
        <Link href="/leagues" className="text-xs font-medium text-primary hover:underline">
          Tout voir
        </Link>
      </CardHeader>
      <CardContent className="flex flex-col gap-1">
        {isLoading &&
          Array.from({ length: 2 }).map((_, i) => <Skeleton key={i} className="h-10 w-full rounded-xl" />)}

        {!isLoading && top.length === 0 && (
          <div className="flex flex-col items-center gap-2 py-4 text-center text-sm text-muted-foreground">
            <Shield className="h-6 w-6" />
            <span>Pas encore de ligue.</span>
            <Link href="/leagues" className="text-xs font-medium text-primary hover:underline">
              Crée-en une ou rejoins celle d&apos;un ami
            </Link>
          </div>
        )}

        {!isLoading &&
          top.map((league) => (
            <Link
              key={league.id}
              href={`/leagues/${league.id}`}
              className="flex items-center gap-3 rounded-xl px-2 py-1.5 transition-colors hover:bg-white/[0.06]"
            >
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
                <Shield className="h-4 w-4" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium">{league.name}</p>
                <p className="flex items-center gap-1 font-mono text-xs text-muted-foreground">
                  <Users className="h-3 w-3" />
                  {league.memberCount} membre{league.memberCount > 1 ? "s" : ""}
                </p>
              </div>
              {league.isOwner && (
                <span className="shrink-0 rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold uppercase text-primary">
                  Créateur
                </span>
              )}
            </Link>
          ))}
      </CardContent>
    </Card>
  );
}
