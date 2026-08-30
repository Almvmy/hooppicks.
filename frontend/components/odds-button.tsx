"use client";

import { TrendingDown, TrendingUp, Users } from "lucide-react";
import { useBetSlip } from "@/components/bet-slip-provider";
import { BetSelection } from "@/lib/types";
import { useOddsTrend } from "@/lib/odds-trend";
import { cn } from "@/lib/utils";

export function OddsButton({
  selection,
  impliedProbability,
  communityPct,
}: {
  selection: BetSelection;
  /** 0-100, uniquement pertinent pour le moneyline (spread/total ont une cote fixe 1.91 des deux côtés, donc toujours ~52% : pas un signal utile). */
  impliedProbability?: number;
  /** 0-100, part des paris de la communauté sur ce côté : null/undefined si personne n'a encore parié sur ce marché. */
  communityPct?: number | null;
}) {
  const { selections, toggleSelection } = useBetSlip();
  const isActive = selections.some((s) => s.id === selection.id);
  const trend = useOddsTrend(selection.id, selection.odds);

  return (
    <button
      type="button"
      onClick={(e) => {
        e.preventDefault();
        toggleSelection(selection);
      }}
      className={cn(
        // min-h-11 = 44px : cible tactile. rounded-xl et non rounded-md.
        // border + bg-secondary/50 → glass-inset (liseré, pas de bordure).
        "flex min-h-11 flex-1 flex-col items-center justify-center rounded-xl px-2 py-1.5 text-xs transition-all",
        isActive ? "glass-accent" : "glass-inset text-muted-foreground"
      )}
    >
      <span className="truncate">{selection.label}</span>
      <span
        className={cn(
          "flex items-center gap-0.5 font-mono font-bold transition-colors duration-500",
          !isActive && "text-foreground",
          trend === "up" && "text-success",
          trend === "down" && "text-destructive"
        )}
      >
        {trend === "up" && <TrendingUp className="h-3 w-3 shrink-0" />}
        {trend === "down" && <TrendingDown className="h-3 w-3 shrink-0" />}
        {selection.odds.toFixed(2)}
      </span>
      {(impliedProbability !== undefined || communityPct !== undefined) && (
        <span className="flex items-center gap-1.5 font-mono text-[10px] text-muted-foreground">
          {impliedProbability !== undefined && <span>{Math.round(impliedProbability)}%</span>}
          {communityPct !== undefined && communityPct !== null && (
            <span className="flex items-center gap-0.5">
              <Users className="h-2.5 w-2.5" />
              {communityPct}%
            </span>
          )}
        </span>
      )}
    </button>
  );
}
