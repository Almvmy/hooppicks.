import { cn } from "@/lib/utils";
import { PlacedBet } from "@/lib/types";

/**
 * Frise "W/L/W/L" façon forme d'équipe (ESPN) sur les derniers paris
 * résolus, du plus ancien (gauche) au plus récent (droite).
 */
export function FormStreak({
  bets,
  limit = 10,
}: {
  bets: PlacedBet[] | undefined;
  limit?: number;
}) {
  const resolved = (bets ?? []).filter((b) => b.status === "won" || b.status === "lost");
  // bets arrive trié du plus récent au plus ancien : on prend les `limit`
  // plus récents puis on inverse pour un affichage chronologique.
  const recent = resolved.slice(0, limit).reverse();

  if (recent.length === 0) {
    return <p className="text-sm text-muted-foreground">Pas encore de paris résolus.</p>;
  }

  return (
    <div className="flex items-center gap-1.5">
      {recent.map((bet) => (
        <span
          key={bet.id}
          title={bet.status === "won" ? "Gagné" : "Perdu"}
          className={cn(
            "flex h-6 w-6 items-center justify-center rounded-full font-mono text-[10px] font-bold",
            bet.status === "won"
              ? "bg-success/20 text-success"
              : "bg-destructive/20 text-destructive"
          )}
        >
          {bet.status === "won" ? "G" : "P"}
        </span>
      ))}
    </div>
  );
}
