import { OddsButton } from "@/components/odds-button";
import { Match } from "@/lib/types";

export function MatchOddsRow({ match }: { match: Match }) {
  const { odds } = match;

  return (
    <div className="mt-3 flex flex-col gap-2" onClick={(e) => e.stopPropagation()}>
      <div className="flex gap-2">
        <OddsButton
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