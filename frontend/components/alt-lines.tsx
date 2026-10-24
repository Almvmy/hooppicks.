"use client";

import { useLayoutEffect, useRef } from "react";
import { OddsButton } from "@/components/odds-button";
import type { AltLine, BetSelection, Match } from "@/lib/types";

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

interface Rung {
  line: number;
  first: BetSelection;
  second: BetSelection;
  firstPct?: number | null;
  secondPct?: number | null;
}

/**
 * Échelle d'un marché (écart ou total) : la ligne principale au milieu, les
 * lignes alternatives (cf. AltLines côté serveur) de part et d'autre, chacune
 * sa cote. Trois colonnes visibles sur téléphone, le reste en défilant de côté.
 */
export function LineLadder({
  match,
  market,
  main,
}: {
  match: Match;
  market: "alt_spread" | "alt_total";
  main: Rung;
}) {
  const scroller = useRef<HTMLDivElement>(null);
  const mainRef = useRef<HTMLDivElement>(null);
  const rungs: Rung[] = [
    main,
    ...(match.altLines ?? [])
      .filter((l) => l.market === market)
      .map((l) =>
        market === "alt_spread"
          ? { line: l.line, first: altSelection(match, l, "away"), second: altSelection(match, l, "home") }
          : { line: l.line, first: altSelection(match, l, "over"), second: altSelection(match, l, "under") }
      ),
  ].sort((a, b) => a.line - b.line);

  // Ligne principale centrée à l'ouverture, sans faire défiler la page.
  useLayoutEffect(() => {
    const box = scroller.current;
    const col = mainRef.current;
    if (!box || !col) return;
    box.scrollLeft = col.offsetLeft - box.offsetLeft - (box.clientWidth - col.clientWidth) / 2;
  }, [rungs.length]);

  return (
    <div ref={scroller} className="glass-scroll flex snap-x gap-2 overflow-x-auto pb-1">
      {rungs.map((r) => (
        <div
          key={r.line}
          ref={r === main ? mainRef : undefined}
          className="flex shrink-0 basis-[calc((100%-1rem)/3)] snap-center flex-col gap-1.5"
        >
          <OddsButton selection={r.first} communityPct={r.firstPct} />
          <OddsButton selection={r.second} communityPct={r.secondPct} />
        </div>
      ))}
    </div>
  );
}
