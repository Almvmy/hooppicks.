import { OddsButton } from "@/components/odds-button";
import { LineLadder } from "@/components/alt-lines";
import { BetSelection, Match } from "@/lib/types";
import { boostedOdds, useBoost } from "@/lib/boost";
import { winChances, formatLine } from "@/lib/utils";

/**
 * Cotes d'un match. `ladder` (page du match) : écart et total en échelle,
 * la ligne principale entourée des lignes alternatives.
 */
export function MatchOddsRow({ match, ladder = false }: { match: Match; ladder?: boolean }) {
  const { odds } = match;
  const matchLabel = `${match.awayTeam.name} vs ${match.homeTeam.name}`;
  const spreadAway: BetSelection = {
    id: `${match.id}-spread-away`,
    matchId: match.id,
    matchLabel,
    market: "spread",
    outcome: "away",
    label: `${match.awayTeam.abbreviation} ${odds.spreadValue > 0 ? "-" : "+"}${formatLine(Math.abs(odds.spreadValue))}`,
    odds: odds.spreadOddsAway,
  };
  const spreadHome: BetSelection = {
    id: `${match.id}-spread-home`,
    matchId: match.id,
    matchLabel,
    market: "spread",
    outcome: "home",
    label: `${match.homeTeam.abbreviation} ${odds.spreadValue > 0 ? "+" : "-"}${formatLine(Math.abs(odds.spreadValue))}`,
    odds: odds.spreadOddsHome,
  };
  const totalOver: BetSelection = {
    id: `${match.id}-total-over`,
    matchId: match.id,
    matchLabel,
    market: "total",
    outcome: "over",
    label: `Plus de ${formatLine(odds.totalValue)}`,
    odds: odds.totalOddsOver,
  };
  const totalUnder: BetSelection = {
    id: `${match.id}-total-under`,
    matchId: match.id,
    matchLabel,
    market: "total",
    outcome: "under",
    label: `Moins de ${formatLine(odds.totalValue)}`,
    odds: odds.totalOddsUnder,
  };
  // Cote boostée : le pari « vainqueur » de ce match payé +15 %.
  const boosted = boostedOdds(useBoost(), match.id);
  // Chances de victoire tirées des cotes, marge retirée (somme = 100 %).
  // Seulement pour le vainqueur : spread/total ont la même cote des deux
  // côtés, le chiffre serait toujours 50 %.
  const [awayImpliedProbability, homeImpliedProbability] = winChances(odds.moneylineAway, odds.moneylineHome);
  const pct = match.pickPercentages;

  return (
    <div className="mt-3 flex flex-col gap-2" onClick={(e) => e.stopPropagation()}>
      <div className="flex gap-2">
        <OddsButton
          impliedProbability={awayImpliedProbability}
          communityPct={pct?.moneylineAwayPct}
          boostedFrom={boosted ? odds.moneylineAway : undefined}
          selection={{
            id: `${match.id}-moneyline-away`,
            matchId: match.id,
            matchLabel: `${match.awayTeam.name} vs ${match.homeTeam.name}`,
            market: "moneyline",
            outcome: "away",
            label: `${match.awayTeam.abbreviation} (V)`,
            odds: boosted ? boosted.awayOdds : odds.moneylineAway,
          }}
        />
        <OddsButton
          impliedProbability={homeImpliedProbability}
          communityPct={pct?.moneylineHomePct}
          boostedFrom={boosted ? odds.moneylineHome : undefined}
          selection={{
            id: `${match.id}-moneyline-home`,
            matchId: match.id,
            matchLabel: `${match.awayTeam.name} vs ${match.homeTeam.name}`,
            market: "moneyline",
            outcome: "home",
            label: `${match.homeTeam.abbreviation} (V)`,
            odds: boosted ? boosted.homeOdds : odds.moneylineHome,
          }}
        />
      </div>
      {ladder && match.altLines?.length ? (
        <>
          <p className="mt-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Écart</p>
          <LineLadder
            match={match}
            market="alt_spread"
            main={{
              line: odds.spreadValue,
              first: spreadAway,
              second: spreadHome,
              firstPct: pct?.spreadAwayPct,
              secondPct: pct?.spreadHomePct,
            }}
          />
          <p className="mt-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Total de points</p>
          <LineLadder
            match={match}
            market="alt_total"
            main={{
              line: odds.totalValue,
              first: totalOver,
              second: totalUnder,
              firstPct: pct?.totalOverPct,
              secondPct: pct?.totalUnderPct,
            }}
          />
        </>
      ) : (
        <>
          <div className="flex gap-2">
            <OddsButton communityPct={pct?.spreadAwayPct} selection={spreadAway} />
            <OddsButton communityPct={pct?.spreadHomePct} selection={spreadHome} />
          </div>
          <div className="flex gap-2">
            <OddsButton communityPct={pct?.totalOverPct} selection={totalOver} />
            <OddsButton communityPct={pct?.totalUnderPct} selection={totalUnder} />
          </div>
        </>
      )}
    </div>
  );
}