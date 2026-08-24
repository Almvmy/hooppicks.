"use client";

import { useQuery } from "@tanstack/react-query";
import { Card, CardContent } from "@/components/ui/card";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { fetchPendingBets } from "@/lib/api/admin";
import { formatRelativeTime } from "@/lib/utils";

export function AdminPendingBetsPanel() {
  const { data: bets, isLoading, isError } = useQuery({
    queryKey: ["admin-pending-bets"],
    queryFn: fetchPendingBets,
  });

  return (
    <Card>
      <CardContent className="flex flex-col gap-4 pt-6">
        <p className="text-xs uppercase tracking-wide text-muted-foreground">
          Paris en attente {bets && `(${bets.length})`}
        </p>

        {isLoading && <BasketballLoader label="Chargement des paris..." />}
        {isError && <p className="text-sm text-destructive">Impossible de charger les paris.</p>}

        {bets && (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Joueur</TableHead>
                  <TableHead>Sélections</TableHead>
                  <TableHead className="text-right">Mise</TableHead>
                  <TableHead className="text-right">Gain potentiel</TableHead>
                  <TableHead className="text-right">Placé</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {bets.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} className="text-center text-muted-foreground">
                      Aucun pari en attente.
                    </TableCell>
                  </TableRow>
                )}
                {bets.map((bet) => (
                  <TableRow key={bet.id}>
                    <TableCell className="font-medium">@{bet.username}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {bet.selections.map((s) => s.label).join(", ")}
                    </TableCell>
                    <TableCell className="text-right font-mono">{bet.stake} pts</TableCell>
                    <TableCell className="text-right font-mono">{bet.potentialPayout} pts</TableCell>
                    <TableCell className="text-right text-xs text-muted-foreground">
                      {formatRelativeTime(bet.placedAt)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
