"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Trophy } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { CountUp } from "@/components/motion/count-up";
import { fetchMyStanding } from "@/lib/api/leaderboard";
import { cn, formatSignedPoints } from "@/lib/utils";

function ordinal(rank: number) {
  return rank === 1 ? "1er" : `${rank}e`;
}

/**
 * Points de classement (bénéfice net de la saison) et rang, en tête de la
 * barre du haut : c'est le score du jeu. Le solde de pari, qui repart à
 * 1 000 chaque lundi, reste dans le ticket et sur le tableau de bord.
 */
export function StandingBadge() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["leaderboard", "me"],
    queryFn: fetchMyStanding,
    // Les paris se règlent en tâche de fond toutes les 5 min.
    refetchInterval: 60_000,
  });

  if (isLoading) return <Skeleton className="h-7 w-24 rounded-full" />;
  if (isError || !data) return null;

  const week =
    data.weekRank === null
      ? "pas encore de ticket réglé cette semaine"
      : `${formatSignedPoints(data.weekPoints)} cette semaine (${ordinal(data.weekRank)})`;
  const title =
    data.seasonRank === null
      ? "Pas encore classé : ton premier ticket réglé te fait entrer au classement."
      : `Classement saison : ${formatSignedPoints(data.seasonPoints)} de bénéfice net, ${ordinal(data.seasonRank)} sur ${data.seasonPlayers}. ${week[0].toUpperCase()}${week.slice(1)}.`;

  return (
    <Link
      href="/leaderboard"
      title={title}
      aria-label={title}
      className={cn(
        "flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-2.5 py-1 font-mono text-sm font-semibold text-primary transition-colors hover:bg-primary/15"
      )}
    >
      <Trophy className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
      <CountUp value={data.seasonPoints} format={(n) => formatSignedPoints(n)} animateOnMount={false} />
      {data.seasonRank !== null && (
        <span className="hidden border-l border-primary/30 pl-1.5 font-sans text-xs sm:inline">
          {ordinal(data.seasonRank)}
        </span>
      )}
    </Link>
  );
}
