import { Trophy } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { PlacedBet } from "@/lib/types";

/**
 * Met en avant le ticket gagnant au plus gros gain, façon carte à
 * collectionner holographique — n'affiche rien tant qu'aucun pari n'est
 * encore gagné (pas de contenu vide à montrer).
 */
export function BestBetTrophy({ bets }: { bets: PlacedBet[] | undefined }) {
  const wonBets = (bets ?? []).filter((b) => b.status === "won");
  if (wonBets.length === 0) return null;

  const best = wonBets.reduce((a, b) => (b.potentialPayout > a.potentialPayout ? b : a));

  return (
    <Card className="badge-holo relative overflow-hidden border-primary/30 bg-gradient-to-br from-primary/10 via-card to-card">
      <div className="pointer-events-none absolute -right-6 -top-6 h-32 w-32 rounded-full bg-primary/20 blur-2xl" />
      <CardContent className="relative flex items-start gap-3 pt-6">
        <Trophy className="h-8 w-8 shrink-0 text-primary" />
        <div className="min-w-0">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Meilleur ticket</p>
          <p className="mt-0.5 font-heading text-xl font-bold text-primary">
            +{best.potentialPayout.toLocaleString("fr-FR")} pts
          </p>
          <p className="mt-1 truncate text-sm text-muted-foreground">
            {best.selections.map((s) => s.label).join(" · ")}
          </p>
        </div>
      </CardContent>
    </Card>
  );
}
