"use client";

import { TrendingDown, TrendingUp } from "lucide-react";
import { useBetSlip } from "@/components/bet-slip-provider";
import { BetSelection } from "@/lib/types";
import { useOddsTrend } from "@/lib/odds-trend";
import { cn } from "@/lib/utils";

export function OddsButton({ selection }: { selection: BetSelection }) {
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
    </button>
  );
}
