import { OddsButton } from "@/components/odds-button";
import { Match } from "@/lib/types";

export function MatchOddsRow({ match }: { match: Match }) {
  const { odds } = match;
  // Probabilité implicite de la cote (avec la marge bookmaker incluse,
  // comme n'importe quel site de paris) : dérivée de la cote déjà stockée,
  // aucun appel ni calcul serveur supplémentaire. Pertinent uniquement pour
  // le moneyline : spread/total ont une cote fixe 1.91 des deux côtés (donc
  // toujours ~52%), afficher ce chiffre partout serait du bruit.
  const awayImpliedProbability = (1 / odds.moneylineAway) * 100;
  const homeImpliedProbability = (1 / odds.moneylineHome) * 100;
  const pct = match.pickPercentages;

  return (
    <div className="mt-3 flex flex-col gap-2" onClick={(e) => e.stopPropagation()}>
      <div className="flex gap-2">
        <OddsButton
          impliedProbability={awayImpliedProbability}
          communityPct={pct?.moneylineAwayPct}
          selection={{
            id: `${match.id}-moneyline-away`,
            matchId: match.id,
            matchLabel: `${match.awayTeam.name} vs ${match.homeTeam.name}`,
            market: "moneyline",
            outcome: "away",
            label: `${match.awayTeam.abbreviation} (V)`,
            odds: odds.moneylineAway,
          }}
        />
        <OddsButton
          impliedProbability={homeImpliedProbability}
          communityPct={pct?.moneylineHomePct}
          selection={{
            id: `${match.id}-moneyline-home`,
            matchId: match.id,
            matchLabel: `${match.awayTeam.name} vs ${match.homeTeam.name}`,
            market: "moneyline",
            outcome: "home",
            label: `${match.homeTeam.abbreviation} (V)`,
            odds: odds.moneylineHome,
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
            label: `${match.awayTeam.abbreviation} ${odds.spreadValue > 0 ? "-" : "+"}${Math.abs(odds.spreadValue)}`,
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
            label: `${match.homeTeam.abbreviation} ${odds.spreadValue > 0 ? "+" : "-"}${Math.abs(odds.spreadValue)}`,
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
            label: `Plus de ${odds.totalValue}`,
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
            label: `Moins de ${odds.totalValue}`,
            odds: odds.totalOddsUnder,
          }}
        />
      </div>
    </div>
  );
}