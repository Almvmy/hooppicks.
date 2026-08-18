"use client";

import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import { PaginationControls, usePagination } from "@/components/ui/pagination-controls";
import { fetchBets } from "@/lib/api/bets";
import { BetStatus } from "@/lib/types";
import { cn } from "@/lib/utils";

const PAGE_SIZE = 8;

const STATUS_CONFIG: Record<BetStatus, { label: string; className: string }> = {
  pending: { label: "En attente", className: "border-border bg-secondary text-muted-foreground" },
  won: { label: "Gagné", className: "border-success/30 bg-success/10 text-success" },
  lost: { label: "Perdu", className: "border-destructive/30 bg-destructive/10 text-destructive" },
  void: { label: "Annulé", className: "border-border bg-secondary text-muted-foreground" },
};

export default function BetsPage() {
  const { data, isLoading, isError } = useQuery({ queryKey: ["bets"], queryFn: fetchBets });
  const { page, pageCount, pageItems, setPage, totalCount } = usePagination(data, PAGE_SIZE);

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-bold">Mes paris</h1>
        <p className="mt-1 text-muted-foreground">
          Historique de tes tickets
          {!isLoading && !isError && totalCount > 0 && ` — ${totalCount} au total`}.
        </p>
      </div>

      <div className="flex flex-col gap-3">
        {isLoading && <BasketballLoader label="Chargement de tes tickets..." />}

        {isError && <p className="text-destructive">Impossible de charger tes paris.</p>}

        {!isLoading && !isError && totalCount === 0 && (
          <p className="text-muted-foreground">Aucun ticket pour l&apos;instant.</p>
        )}

        {!isLoading &&
          !isError &&
          pageItems.map((bet) => (
            <Card key={bet.id} className="border-border bg-card">
              <CardContent className="flex flex-col gap-2 pt-6">
                <div className="flex items-center justify-between">
                  <Badge variant="outline" className={cn(STATUS_CONFIG[bet.status].className)}>
                    {STATUS_CONFIG[bet.status].label}
                  </Badge>
                  <span className="font-mono text-xs text-muted-foreground">
                    {new Date(bet.placedAt).toLocaleDateString("fr-FR", {
                      day: "2-digit", month: "short", year: "numeric",
                    })}
                  </span>
                </div>

                {bet.selections.map((s) => (
                  <div key={s.id} className="flex items-center justify-between text-sm">
                    <span>
                      <span className="text-muted-foreground">{s.matchLabel} — </span>
                      {s.label}
                    </span>
                    <span className="font-mono">{s.odds.toFixed(2)}</span>
                  </div>
                ))}

                <div className="mt-2 flex items-center justify-between border-t border-border pt-2 text-sm">
                  <span className="text-muted-foreground">
                    Mise {bet.stake.toLocaleString("fr-FR")} pts · Cote totale {bet.totalOdds.toFixed(2)}
                  </span>
                  <span
                    className={cn(
                      "font-mono font-bold",
                      bet.status === "won" ? "text-success" : "text-foreground"
                    )}
                  >
                    {bet.status === "won" ? "+" : ""}
                    {bet.potentialPayout.toLocaleString("fr-FR")} pts
                  </span>
                </div>
              </CardContent>
            </Card>
          ))}

        {!isLoading && !isError && totalCount > 0 && (
          <PaginationControls page={page} pageCount={pageCount} onPageChange={setPage} />
        )}
      </div>
    </div>
  );
}
