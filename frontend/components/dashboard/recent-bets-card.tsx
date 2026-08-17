"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, Ticket } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchBets } from "@/lib/mock/bets";
import { BetStatus } from "@/lib/types";
import { cn } from "@/lib/utils";

const STATUS_CONFIG: Record<BetStatus, { label: string; className: string }> = {
  pending: { label: "En attente", className: "border-border bg-secondary text-muted-foreground" },
  won: { label: "Gagné", className: "border-success/30 bg-success/10 text-success" },
  lost: { label: "Perdu", className: "border-destructive/30 bg-destructive/10 text-destructive" },
  void: { label: "Annulé", className: "border-border bg-secondary text-muted-foreground" },
};

export function RecentBetsCard() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["bets-dashboard"],
    queryFn: fetchBets,
  });

  const recent = [...(data ?? [])]
    .sort((a, b) => new Date(b.placedAt).getTime() - new Date(a.placedAt).getTime())
    .slice(0, 4);

  return (
    <Card className="border-border bg-card">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="flex items-center gap-2 font-heading text-lg">
          <Ticket className="h-4 w-4 text-primary" />
          Derniers paris
        </CardTitle>
        <Link
          href="/bets"
          className="flex items-center gap-1 text-xs font-medium text-muted-foreground transition-colors hover:text-primary"
        >
          Tout voir
          <ArrowRight className="h-3 w-3" />
        </Link>
      </CardHeader>
      <CardContent className="flex flex-col gap-2">
        {isLoading &&
          Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-12 w-full rounded-lg" />)}

        {isError && <p className="text-sm text-destructive">Impossible de charger tes paris.</p>}

        {!isLoading && !isError && recent.length === 0 && (
          <p className="text-sm text-muted-foreground">Aucun ticket pour l&apos;instant.</p>
        )}

        {!isLoading &&
          !isError &&
          recent.map((bet) => (
            <div
              key={bet.id}
              className="flex items-center justify-between gap-3 rounded-lg border border-border/60 bg-secondary/20 px-3 py-2 text-sm"
            >
              <div className="min-w-0">
                <p className="truncate font-medium">{bet.selections[0]?.matchLabel}</p>
                <p className="truncate text-xs text-muted-foreground">{bet.selections[0]?.label}</p>
              </div>
              <div className="flex shrink-0 flex-col items-end gap-1">
                <Badge variant="outline" className={cn("text-xs", STATUS_CONFIG[bet.status].className)}>
                  {STATUS_CONFIG[bet.status].label}
                </Badge>
                <span className="font-mono text-xs text-muted-foreground">
                  {bet.stake.toLocaleString("fr-FR")} pts
                </span>
              </div>
            </div>
          ))}
      </CardContent>
    </Card>
  );
}
