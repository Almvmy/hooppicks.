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
  /** 0-100, chances de victoire marge retirée (cf. winChances), uniquement pour le vainqueur : spread/total ont la même cote des deux côtés, donc toujours 50 %. */
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
          {/* Deux chiffres de nature différente : chacun porte sa légende
              (texte ou icône + infobulle), sinon on les confondait. */}
          {impliedProbability !== undefined && (
            <span title="Chances de victoire selon les cotes">{Math.round(impliedProbability)}% chances</span>
          )}
          {communityPct !== undefined && communityPct !== null && (
            <span
              className="flex items-center gap-0.5"
              title="Part des joueurs HoopPicks qui ont choisi ce camp"
              aria-label={`${communityPct}% des joueurs ont choisi ce camp`}
            >
              <Users className="h-2.5 w-2.5" aria-hidden="true" />
              {communityPct}%
            </span>
          )}
        </span>
      )}
    </button>
  );
}
