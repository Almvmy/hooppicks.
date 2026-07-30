"use client";

import { useBetSlip } from "@/components/bet-slip-provider";
import { BetSelection } from "@/lib/types";
import { cn } from "@/lib/utils";

export function OddsButton({ selection }: { selection: BetSelection }) {
  const { selections, toggleSelection } = useBetSlip();
  const isActive = selections.some((s) => s.id === selection.id);

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
      <span className="font-mono font-bold">{selection.odds.toFixed(2)}</span>
    </button>
  );
}