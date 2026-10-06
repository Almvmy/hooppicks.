import { Crown, FlaskConical, Star, Ticket, Trophy } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { Match, MatchType } from "@/lib/types";
import { cn } from "@/lib/utils";

export const MATCH_TYPE_META: Record<MatchType, { label: string; icon: LucideIcon | null; className: string }> = {
  regular: { label: "Saison régulière", icon: null, className: "glass-inset-quiet text-muted-foreground" },
  preseason: { label: "Présaison", icon: FlaskConical, className: "glass-inset-quiet text-muted-foreground" },
  nba_cup: { label: "Coupe NBA", icon: Trophy, className: "bg-amber-400/15 text-amber-400 light:text-amber-800" },
  all_star: { label: "All-Star", icon: Star, className: "bg-sky-400/15 text-sky-400 light:text-sky-800" },
  play_in: { label: "Play-in", icon: Ticket, className: "bg-paint/15 text-paint" },
  playoffs: { label: "Playoffs", icon: Crown, className: "bg-brand/15 text-primary" },
};

/**
 * Phase du match (présaison, Coupe NBA, playoffs…) avec son intitulé
 * ("1er tour Est · Match 2"). Rien pour la saison régulière : c'est le cas
 * par défaut, l'afficher partout ne serait que du bruit.
 */
export function MatchStageBadge({ match, className }: { match: Match; className?: string }) {
  if (!match.type || match.type === "regular") return null;
  const meta = MATCH_TYPE_META[match.type];
  const Icon = meta.icon;
  return (
    <span className={cn("flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1", className)}>
      <span
        className={cn(
          "flex min-w-0 items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide",
          meta.className
        )}
      >
        {Icon && <Icon className="h-3 w-3 shrink-0" />}
        <span className="truncate">{match.stageLabel ?? meta.label}</span>
      </span>
      {match.seriesSummary && (
        <span className="font-mono text-[11px] font-semibold text-muted-foreground">{match.seriesSummary}</span>
      )}
    </span>
  );
}
