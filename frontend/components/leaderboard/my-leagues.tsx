"use client";

import Link from "next/link";
import { useQueries, useQuery } from "@tanstack/react-query";
import { ChevronRight, Plus, Shield } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { LeagueCrest } from "@/components/league-crest";
import { fetchLeagueLeaderboard, fetchMyLeagues } from "@/lib/api/leagues";
import { cn } from "@/lib/utils";

/** Ta place dans chacune de tes ligues, sans passer par la page Ligues. */
export function MyLeagues({ username }: { username: string | undefined }) {
  const { data: leagues, isLoading, isError } = useQuery({ queryKey: ["leagues"], queryFn: fetchMyLeagues });
  // Mêmes clés que la page Ligues : les classements déjà chargés là-bas servent ici.
  const boards = useQueries({
    queries: (leagues ?? []).map((l) => ({
      queryKey: ["league-leaderboard", l.id],
      queryFn: () => fetchLeagueLeaderboard(l.id),
      staleTime: 60 * 1000,
    })),
  });

  if (isLoading) {
    return (
      <div className="grid gap-3 sm:grid-cols-2">
        {Array.from({ length: 2 }).map((_, i) => (
          <Skeleton key={i} className="h-24 rounded-2xl" />
        ))}
      </div>
    );
  }
  if (isError) return <p className="text-destructive">Impossible de charger tes ligues.</p>;

  if (!leagues || leagues.length === 0) {
    return (
      <div className="glass flex flex-col items-center gap-3 rounded-2xl py-10 text-center">
        <Shield className="h-8 w-8 text-muted-foreground" />
        <p className="max-w-sm text-sm text-muted-foreground">
          Tu n&apos;es dans aucune ligue. Crée la tienne et invite tes potes pour un classement entre vous.
        </p>
        <Link href="/leagues" className="flex items-center gap-1 text-sm font-medium text-primary hover:underline">
          <Plus className="h-4 w-4" />
          Créer ou rejoindre une ligue
        </Link>
      </div>
    );
  }

  return (
    <div className="stagger-children grid gap-3 sm:grid-cols-2">
      {leagues.map((league, i) => {
        const board = boards[i];
        const entries = board?.data ?? [];
        const index = entries.findIndex((e) => e.username === username);
        const me = index >= 0 ? entries[index] : undefined;
        const leader = entries[0];
        const ahead = index > 0 ? entries.slice(0, index).reverse().find((e) => e.points > (me?.points ?? 0)) : undefined;

        return (
          <Link
            key={league.id}
            href={`/leagues/${league.id}`}
            className="glass group flex items-center gap-3 rounded-2xl p-4 transition-colors hover:bg-tint/[0.04]"
          >
            <LeagueCrest name={league.name} size={44} />
            <div className="min-w-0 flex-1">
              <p className="truncate font-medium">{league.name}</p>
              <p className="truncate text-xs text-muted-foreground">
                {board?.isLoading
                  ? "Chargement…"
                  : board?.isError
                    ? "Classement indisponible"
                    : !me
                      ? "Pas encore classé : un ticket résolu suffit"
                      : me.rank === 1
                        ? `En tête · ${league.memberCount} membre${league.memberCount > 1 ? "s" : ""}`
                        : ahead
                          ? `${(ahead.points - me.points).toLocaleString("fr-FR")} pts derrière ${ahead.username}`
                          : `À égalité avec ${leader?.username ?? "le leader"}`}
              </p>
            </div>
            <span className="shrink-0 text-right">
              <span className={cn("block font-heading text-2xl font-bold tabular-nums", me?.rank === 1 && "text-primary")}>
                {me ? `#${me.rank}` : "-"}
              </span>
              <span className="block text-[10px] uppercase text-muted-foreground">
                sur {league.memberCount}
              </span>
            </span>
            <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-0.5" />
          </Link>
        );
      })}
    </div>
  );
}
