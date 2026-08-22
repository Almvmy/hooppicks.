import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { BetStatus, PlacedBet } from "@/lib/types";
import { cn } from "@/lib/utils";

const STATUS_CONFIG: Record<BetStatus, { label: string; variant: "secondary" | "success" | "destructive" }> = {
  pending: { label: "En attente", variant: "secondary" },
  won: { label: "Gagné", variant: "success" },
  lost: { label: "Perdu", variant: "destructive" },
  void: { label: "Annulé", variant: "secondary" },
};

export function RecentActivity({
  bets,
  isLoading,
}: {
  bets: PlacedBet[];
  isLoading: boolean;
}) {
  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle className="font-heading text-base">Derniers tickets</CardTitle>
        <Link href="/bets" className="text-xs font-medium text-primary hover:underline">
          Tout voir
        </Link>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {isLoading &&
          Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-16 w-full rounded-md" />
          ))}

        {!isLoading && bets.length === 0 && (
          <p className="py-4 text-center text-sm text-muted-foreground">
            Aucun ticket pour l&apos;instant. Lance ton premier pronostic !
          </p>
        )}

        {!isLoading &&
          bets.map((bet) => (
            <div key={bet.id} className="glass-inset-quiet rounded-xl p-3">
              <div className="flex items-center justify-between">
                <Badge variant={STATUS_CONFIG[bet.status].variant} className="text-[11px]">
                  {STATUS_CONFIG[bet.status].label}
                </Badge>
                <span
                  className={cn(
                    "font-mono text-sm font-bold",
                    bet.status === "won" ? "text-success" : "text-foreground"
                  )}
                >
                  {bet.status === "won" ? "+" : ""}
                  {bet.potentialPayout.toLocaleString("fr-FR")} pts
                </span>
              </div>
              <p className="mt-1.5 truncate text-sm text-muted-foreground">
                {bet.selections.map((s) => s.label).join(" · ")}
              </p>
            </div>
          ))}
      </CardContent>
    </Card>
  );
}
