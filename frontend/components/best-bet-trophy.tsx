import { Trophy } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { PlacedBet } from "@/lib/types";
import { formatOdds, formatSignedPoints } from "@/lib/utils";

/**
 * Met en avant le ticket gagnant au plus gros bénéfice (gain moins mise,
 * comme au classement), façon carte à collectionner holographique :
 * n'affiche rien tant qu'aucun pari n'est encore gagné.
 */
export function BestBetTrophy({ bets }: { bets: PlacedBet[] | undefined }) {
  const wonBets = (bets ?? []).filter((b) => b.status === "won");
  if (wonBets.length === 0) return null;

  const net = (b: PlacedBet) => b.potentialPayout - b.stake;
  const best = wonBets.reduce((a, b) => (net(b) > net(a) ? b : a));

  return (
    <Card className="badge-holo relative overflow-hidden border-primary/30 bg-gradient-to-br from-primary/10 via-card to-card">
      <div aria-hidden className="decor-clip">
        <div className="absolute -right-6 -top-6 h-32 w-32 rounded-full bg-primary/20 blur-2xl" />
      </div>
      <CardContent className="relative flex items-start gap-3 pt-6">
        <Trophy className="h-8 w-8 shrink-0 text-primary" />
        <div className="min-w-0">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Meilleur ticket</p>
          <p className="mt-0.5 font-heading text-xl font-bold text-primary">{formatSignedPoints(net(best))}</p>
          <ul className="mt-1 text-sm text-muted-foreground">
            {best.selections.map((s) => (
              <li key={s.id} className="mt-1 break-words first:mt-0">
                <span className="text-foreground">{s.label}</span>
                <span className="block text-xs">{s.matchLabel}</span>
              </li>
            ))}
          </ul>
          <p className="mt-0.5 font-mono text-xs text-muted-foreground">
            mise {best.stake.toLocaleString("fr-FR")} · cote {formatOdds(best.totalOdds)}
          </p>
        </div>
      </CardContent>
    </Card>
  );
}
