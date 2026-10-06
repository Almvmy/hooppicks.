import { ArrowDown, ArrowUp, Flame, Snowflake } from "lucide-react";
import { LeaderboardEntry } from "@/lib/types";
import { cn } from "@/lib/utils";

/** ↑3 / ↓1 depuis la veille, « Nouveau » pour un joueur qui vient d'entrer au classement. */
export function RankChange({ entry, className }: { entry: LeaderboardEntry; className?: string }) {
  if (entry.newcomer) {
    return (
      <span className={cn("rounded-full bg-paint/15 px-1.5 py-px text-[9px] font-bold uppercase text-paint", className)}>
        Nouveau
      </span>
    );
  }
  const change = entry.rankChange;
  if (change === null || change === 0) return null;
  const up = change > 0;
  const Icon = up ? ArrowUp : ArrowDown;
  const places = Math.abs(change);
  const s = places > 1 ? "s" : "";
  return (
    <span
      className={cn("inline-flex items-center font-mono text-[11px] font-bold", up ? "text-success" : "text-destructive", className)}
      title={`${places} place${s} ${up ? "gagnée" : "perdue"}${s} depuis hier`}
    >
      <Icon className="h-3 w-3" aria-hidden />
      {places}
      <span className="sr-only">{` place${s} ${up ? "gagnée" : "perdue"}${s} depuis hier`}</span>
    </span>
  );
}

/** Série en cours, affichée à partir de 3 tickets d'affilée : en dessous, ce n'est pas une série. */
export function StreakBadge({ streak, className }: { streak: number; className?: string }) {
  if (Math.abs(streak) < 3) return null;
  const hot = streak > 0;
  const Icon = hot ? Flame : Snowflake;
  return (
    <span
      className={cn(
        "inline-flex items-center gap-0.5 rounded-full px-1.5 py-px text-[10px] font-bold",
        hot ? "bg-brand/15 text-primary" : "bg-sky-400/15 text-sky-400 light:text-sky-800",
        className
      )}
      title={hot ? `${streak} tickets gagnés d'affilée` : `${-streak} tickets perdus d'affilée`}
    >
      <Icon className="h-3 w-3" aria-hidden />
      {Math.abs(streak)}
    </span>
  );
}

/** 5 derniers tickets, le plus récent à droite (sens de lecture d'une série de résultats). */
export function RecentForm({ form, className }: { form: ("W" | "L")[]; className?: string }) {
  if (form.length === 0) return <span className={cn("text-xs text-muted-foreground", className)}>-</span>;
  const chronological = [...form].reverse();
  const label = chronological.map((r) => (r === "W" ? "gagné" : "perdu")).join(", ");
  return (
    <span className={cn("inline-flex items-center gap-0.5", className)} aria-label={`Derniers tickets : ${label}`} role="img">
      {chronological.map((r, i) => (
        <span
          key={i}
          aria-hidden
          className={cn(
            "flex h-4 w-4 items-center justify-center rounded-[5px] text-[9px] font-bold",
            r === "W" ? "bg-success/20 text-success" : "bg-destructive/15 text-destructive"
          )}
        >
          {r === "W" ? "G" : "P"}
        </span>
      ))}
    </span>
  );
}
