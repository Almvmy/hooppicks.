"use client";

import { Flame } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { CourtWatermark } from "@/components/court-watermark";
import { CountUp } from "@/components/motion/count-up";
import { greetingForNow } from "@/lib/dashboard";
import type { MyStanding } from "@/lib/api/leaderboard";
import { WEEKLY_BANKROLL, cn, formatBankrollReset, formatSignedPoints } from "@/lib/utils";

const WIDTH = 120;
const HEIGHT = 40;

/** Bénéfice cumulé de la semaine, ticket après ticket. */
function Sparkline({ series }: { series: number[] }) {
  if (series.length < 2 || series.every((v) => v === 0)) return null;
  const min = Math.min(...series);
  const max = Math.max(...series);
  const range = max - min || 1;
  const step = WIDTH / (series.length - 1);
  const line = series
    .map((v, i) => `${i === 0 ? "M" : "L"}${(i * step).toFixed(1)},${(HEIGHT - ((v - min) / range) * (HEIGHT - 6) - 3).toFixed(1)}`)
    .join(" ");
  const up = series[series.length - 1] >= 0;
  return (
    <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} className="h-10 w-28 shrink-0" preserveAspectRatio="none" aria-hidden>
      <path d={line} fill="none" stroke={up ? "var(--success)" : "var(--destructive)"} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

/**
 * Haut de l'accueil : la semaine de jeu en un coup d'œil (points et place de
 * la semaine, budget restant, ce qui est en jeu, fin de semaine). Remplace le
 * bandeau de bienvenue, les quatre tuiles et la carte « Ta semaine », qui
 * répétaient les mêmes chiffres à trois endroits.
 */
export function WeekCard({
  username,
  standing,
  balance,
  series,
  pendingCount,
  pendingStake,
  streak,
  isLoading,
}: {
  username: string | undefined;
  standing: MyStanding | undefined;
  balance: number | undefined;
  series: number[];
  pendingCount: number;
  pendingStake: number;
  streak: number;
  isLoading: boolean;
}) {
  const budgetShare = balance === undefined ? 0 : Math.max(0, Math.min(100, (balance / WEEKLY_BANKROLL) * 100));

  return (
    <div className="glass relative overflow-hidden rounded-3xl p-4 sm:p-6">
      <CourtWatermark />
      <div className="relative flex items-center justify-between gap-3">
        {isLoading ? (
          <Skeleton className="h-6 w-48" />
        ) : (
          <h1 className="truncate font-heading text-lg font-bold sm:text-xl">
            {greetingForNow()}, {username ?? "champion"}
          </h1>
        )}
        {streak > 0 && (
          <span
            className={cn(
              "flex shrink-0 items-center gap-1.5 rounded-full px-2.5 py-1 text-xs",
              streak >= 3 ? "glass-accent" : "glass-inset"
            )}
            title={`${streak} ticket${streak > 1 ? "s" : ""} gagné${streak > 1 ? "s" : ""} d'affilée`}
          >
            <Flame className={cn("h-3.5 w-3.5", streak >= 3 ? "text-primary" : "text-muted-foreground")} />
            <span className="font-mono font-bold">{streak}</span>
            série
          </span>
        )}
      </div>

      <div className="relative mt-4 flex items-end justify-between gap-4">
        <div className="min-w-0">
          <p className="text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">Points de la semaine</p>
          {isLoading ? (
            <Skeleton className="mt-1 h-9 w-32" />
          ) : (
            <p className="font-heading text-4xl font-bold tabular-nums leading-tight">
              <CountUp value={standing?.weekPoints ?? 0} format={(n) => formatSignedPoints(n)} />
            </p>
          )}
          <p className="text-sm text-muted-foreground">
            {standing?.weekRank != null
              ? `#${standing.weekRank} sur ${standing.weekPlayers} cette semaine`
              : "Pas encore classé cette semaine"}
          </p>
        </div>
        <Sparkline series={series} />
      </div>

      <div className="relative mt-4 flex flex-col gap-1.5">
        <div className="flex items-baseline justify-between text-xs">
          <span className="text-muted-foreground">
            Reste à miser{" "}
            <span className="font-mono font-bold text-foreground">
              {balance === undefined ? "…" : `${balance.toLocaleString("fr-FR")} pts`}
            </span>
          </span>
          {pendingCount > 0 && (
            <span className="text-muted-foreground">
              {pendingCount} ticket{pendingCount > 1 ? "s" : ""} en jeu ·{" "}
              <span className="font-mono text-foreground">{pendingStake.toLocaleString("fr-FR")} pts</span>
            </span>
          )}
        </div>
        <div className="h-1.5 overflow-hidden rounded-full bg-tint/10" aria-hidden>
          <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${budgetShare}%` }} />
        </div>
        <p className="text-[11px] text-muted-foreground">
          {WEEKLY_BANKROLL.toLocaleString("fr-FR")} pts pour tout le monde {formatBankrollReset()}
        </p>
      </div>
    </div>
  );
}
