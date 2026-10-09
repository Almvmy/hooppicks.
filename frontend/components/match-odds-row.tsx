import { OddsButton } from "@/components/odds-button";
import { Match } from "@/lib/types";
import { useBoost } from "@/lib/boost";
import { winChances, formatLine } from "@/lib/utils";

export function MatchOddsRow({ match }: { match: Match }) {
  const { odds } = match;
  // Cote boostée du jour : le pari « vainqueur » de ce match payé +15 %.
  const boost = useBoost();
  const boosted = boost?.matchId === match.id ? boost : null;
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
      <div className="flex gap-2">
        <OddsButton
          communityPct={pct?.spreadAwayPct}
          selection={{
            id: `${match.id}-spread-away`,
            matchId: match.id,
            matchLabel: `${match.awayTeam.name} vs ${match.homeTeam.name}`,
            market: "spread",
            outcome: "away",
            label: `${match.awayTeam.abbreviation} ${odds.spreadValue > 0 ? "-" : "+"}${formatLine(Math.abs(odds.spreadValue))}`,
            odds: odds.spreadOddsAway,
          }}
        />
        <OddsButton
          communityPct={pct?.spreadHomePct}
          selection={{
            id: `${match.id}-spread-home`,
            matchId: match.id,
            matchLabel: `${match.awayTeam.name} vs ${match.homeTeam.name}`,
            market: "spread",
            outcome: "home",
            label: `${match.homeTeam.abbreviation} ${odds.spreadValue > 0 ? "+" : "-"}${formatLine(Math.abs(odds.spreadValue))}`,
            odds: odds.spreadOddsHome,
          }}
        />
      </div>
      <div className="flex gap-2">
        <OddsButton
          communityPct={pct?.totalOverPct}
          selection={{
            id: `${match.id}-total-over`,
            matchId: match.id,
            matchLabel: `${match.awayTeam.name} vs ${match.homeTeam.name}`,
            market: "total",
            outcome: "over",
            label: `Plus de ${formatLine(odds.totalValue)}`,
            odds: odds.totalOddsOver,
          }}
        />
        <OddsButton
          communityPct={pct?.totalUnderPct}
          selection={{
            id: `${match.id}-total-under`,
            matchId: match.id,
            matchLabel: `${match.awayTeam.name} vs ${match.homeTeam.name}`,
            market: "total",
            outcome: "under",
            label: `Moins de ${formatLine(odds.totalValue)}`,
            odds: odds.totalOddsUnder,
          }}
        />
      </div>
    </div>
  );
}