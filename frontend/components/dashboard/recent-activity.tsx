import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { BetStatus, PlacedBet } from "@/lib/types";
import { cn, formatSignedPoints } from "@/lib/utils";
import { SelectionTeamLogo, useMatchesById } from "@/components/selection-team-logo";

const STATUS_CONFIG: Record<BetStatus, { label: string; variant: "secondary" | "success" | "destructive" }> = {
  pending: { label: "En attente", variant: "secondary" },
  won: { label: "Gagné", variant: "success" },
  lost: { label: "Perdu", variant: "destructive" },
  void: { label: "Remboursé", variant: "secondary" },
};

export function RecentActivity({
  bets,
  isLoading,
}: {
  bets: PlacedBet[];
  isLoading: boolean;
}) {
  const matchesById = useMatchesById();

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
                    bet.status === "won" ? "text-success" : bet.status === "lost" ? "text-destructive" : "text-foreground"
                  )}
                >
                  {/* Même lecture que le classement : bénéfice net du ticket.
                      Avant, un ticket remboursé affichait son gain potentiel. */}
                  {bet.status === "won"
                    ? formatSignedPoints(bet.potentialPayout - bet.stake)
                    : bet.status === "lost"
                      ? formatSignedPoints(-bet.stake)
                      : bet.status === "void"
                        ? "mise rendue"
                        : `${bet.stake.toLocaleString("fr-FR")} pts misés`}
                </span>
              </div>
              <div className="mt-1.5 flex min-w-0 items-center gap-2">
                <span className="flex shrink-0 items-center gap-1">
                  {bet.selections.slice(0, 4).map((s) => (
                    <SelectionTeamLogo key={s.id} selection={s} matchesById={matchesById} size={20} />
                  ))}
                </span>
                <p className="truncate text-sm text-muted-foreground">
                  {bet.selections.map((s) => s.label).join(" · ")}
                </p>
              </div>
            </div>
          ))}
      </CardContent>
    </Card>
  );
}
