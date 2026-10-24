"use client";

import { useState } from "react";
import { ChevronDown } from "lucide-react";
import { OddsButton } from "@/components/odds-button";
import type { AltLine, BetSelection, Match } from "@/lib/types";
import { cn } from "@/lib/utils";

const decimal = (n: number) => n.toLocaleString("fr-FR", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const signed = (n: number) => `${n > 0 ? "+" : n < 0 ? "-" : ""}${decimal(Math.abs(n))}`;

/** Sélection d'une ligne alternative ; handicap exprimé côté domicile, comme le serveur. */
function altSelection(match: Match, alt: AltLine, outcome: BetSelection["outcome"]): BetSelection {
  const isSpread = alt.market === "alt_spread";
  const label = isSpread
    ? outcome === "home"
      ? `${match.homeTeam.abbreviation} ${signed(alt.line)}`
      : `${match.awayTeam.abbreviation} ${signed(-alt.line)}`
    : `${outcome === "over" ? "Plus" : "Moins"} de ${decimal(alt.line)}`;
  return {
    id: `${match.id}-${alt.market}-${alt.line}-${outcome}`,
    matchId: match.id,
    matchLabel: `${match.awayTeam.name} vs ${match.homeTeam.name}`,
    market: alt.market,
    outcome,
    label,
    odds: outcome === "home" || outcome === "over" ? alt.firstOdds : alt.secondOdds,
    line: alt.line,
  };
}

/**
 * Handicaps et totaux alternatifs (cf. AltLines côté serveur) : chacun sa
 * cote, pour choisir son risque. Repliés par défaut pour ne pas charger la
 * page du match.
 */
export function AltLinesSection({ match }: { match: Match }) {
  const [open, setOpen] = useState(false);
  const lines = match.altLines ?? [];
  if (lines.length === 0) return null;
  const spreads = lines.filter((l) => l.market === "alt_spread");
  const totals = lines.filter((l) => l.market === "alt_total");

  return (
    <div className="mt-3 flex flex-col gap-2">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        className="flex items-center justify-center gap-1 rounded-xl py-1 text-xs font-semibold text-muted-foreground transition-colors hover:text-foreground"
      >
        {open ? "Moins de lignes" : "Plus d'écarts et de totaux"}
        <ChevronDown className={cn("h-3.5 w-3.5 transition-transform", open && "rotate-180")} />
      </button>
      {open && (
        <>
          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Écart</p>
          {spreads.map((l) => {
            const away = altSelection(match, l, "away");
            const home = altSelection(match, l, "home");
            return (
              <div key={`s${l.line}`} className="flex gap-2">
                <OddsButton selection={away} label={away.label} />
                <OddsButton selection={home} label={home.label} />
              </div>
            );
          })}
          <p className="mt-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Total de points</p>
          {totals.map((l) => {
            const over = altSelection(match, l, "over");
            const under = altSelection(match, l, "under");
            return (
              <div key={`t${l.line}`} className="flex gap-2">
                <OddsButton selection={over} label={over.label} />
                <OddsButton selection={under} label={under.label} />
              </div>
            );
          })}
        </>
      )}
    </div>
  );
}
