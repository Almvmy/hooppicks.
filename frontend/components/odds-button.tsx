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
        e.preventDefault(); // empêche la navigation si le bouton est dans un <Link> de carte
        toggleSelection(selection);
      }}
      className={cn(
        "flex flex-1 flex-col items-center rounded-md border px-2 py-1.5 text-xs transition-colors",
        isActive
          ? "border-primary bg-primary/15 text-primary"
          : "border-border bg-secondary/50 text-muted-foreground hover:border-primary/40 hover:text-foreground"
      )}
    >
      <span className="truncate">{selection.label}</span>
      <span
        className={cn(
          "flex items-center gap-0.5 font-mono font-bold transition-colors duration-500",
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