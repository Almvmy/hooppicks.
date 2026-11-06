"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { fetchTopPerformances } from "@/lib/api/players";
import { getTeamColor } from "@/lib/team-colors";

function Stat({ value, label }: { value: number; label: string }) {
  return (
    <span className="flex flex-col">
      <span className="font-mono text-2xl font-bold leading-none tabular-nums">{value}</span>
      <span className="mt-1 text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">{label}</span>
    </span>
  );
}

/**
 * La meilleure ligne de la dernière soirée, photo du joueur en grand : ce dont
 * on parle le matin. Carte en plus, pas une information attendue : rien si la
 * source ne répond pas, le reste de l'accueil suffit.
 */
export function NightPerformance() {
  const { data } = useQuery({
    queryKey: ["players", "top-performances"],
    queryFn: fetchTopPerformances,
    staleTime: 10 * 60 * 1000,
  });
  const top = data?.[0];
  if (!top) return null;
  const abbreviation = top.player?.team?.abbreviation ?? top.teamAbbreviation ?? "";
  const color = getTeamColor(abbreviation);

  return (
    <Link href={`/matches/${top.matchId}`} className="glass relative block overflow-hidden rounded-3xl transition-shadow hover:shadow-[var(--lift)]">
      <div aria-hidden className="absolute inset-0" style={{ background: `radial-gradient(90% 120% at 100% 100%, ${color}66 0%, ${color}1f 50%, transparent 75%)` }} />
      {top.player?.headshotUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={top.player.headshotUrl} alt="" loading="lazy" className="absolute -right-5 bottom-0 h-40 w-auto max-w-[62%] object-contain object-bottom sm:h-44" />
      )}
      <div className="relative flex min-h-40 flex-col justify-between gap-4 p-4 sm:min-h-44 sm:p-5">
        <div className="min-w-0 pr-28 sm:pr-40">
          <p className="text-[11px] font-semibold uppercase tracking-wide text-primary">La perf de la nuit</p>
          <p className="mt-1 font-heading text-xl font-bold leading-tight sm:text-2xl">{top.playerName}</p>
          <p className="mt-0.5 text-xs text-muted-foreground">
            {top.teamAbbreviation} contre {top.opponentAbbreviation} · {top.minutes} min
          </p>
        </div>
        <div className="flex gap-5">
          <Stat value={top.points} label="pts" />
          <Stat value={top.rebounds} label="reb" />
          <Stat value={top.assists} label="pas" />
        </div>
      </div>
    </Link>
  );
}
