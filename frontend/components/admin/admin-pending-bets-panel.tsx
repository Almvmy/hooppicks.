"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Ban, CheckCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { ReasonDialog } from "@/components/admin/reason-dialog";
import { fetchPendingBets, resolveBets, voidBet } from "@/lib/api/admin";
import { AdminBet } from "@/lib/types";
import { formatRelativeTime } from "@/lib/utils";

export function AdminPendingBetsPanel() {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState("");
  const [voiding, setVoiding] = useState<AdminBet | null>(null);
  const [confirmResolve, setConfirmResolve] = useState(false);

  const { data: bets, isLoading, isError } = useQuery({
    queryKey: ["admin-pending-bets"],
    queryFn: fetchPendingBets,
  });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ["admin-pending-bets"] });
    queryClient.invalidateQueries({ queryKey: ["admin-overview"] });
    queryClient.invalidateQueries({ queryKey: ["admin-matches"] });
    queryClient.invalidateQueries({ queryKey: ["admin-audit"] });
  };

  const voidMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => voidBet(id, reason),
    onSuccess: () => {
      toast.success("Pari annulé et remboursé.");
      refresh();
    },
    onError: (err: Error) => toast.error(err.message || "Annulation impossible."),
  });

  const resolveMutation = useMutation({
    mutationFn: resolveBets,
    onSuccess: (r) => {
      toast.success(`${r.resolved} pari(s) résolu(s).`);
      refresh();
    },
    onError: () => toast.error("Échec de la résolution des paris."),
  });

  const needle = filter.trim().toLowerCase();
  const shown = (bets ?? []).filter(
    (b) =>
      !needle ||
      b.username.toLowerCase().includes(needle) ||
      b.selections.some((s) => s.matchLabel.toLowerCase().includes(needle) || s.label.toLowerCase().includes(needle))
  );
  const totalStake = (bets ?? []).reduce((sum, b) => sum + b.stake, 0);
  const totalPayout = (bets ?? []).reduce((sum, b) => sum + b.potentialPayout, 0);

  return (
    <Card>
      <CardContent className="flex flex-col gap-4 pt-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-xs uppercase tracking-wide text-muted-foreground">
              Paris en attente {bets && `(${bets.length})`}
            </p>
            {bets && bets.length > 0 && (
              <p className="mt-0.5 text-sm text-muted-foreground">
                {totalStake.toLocaleString("fr-FR")} pts engagés · jusqu&apos;à {totalPayout.toLocaleString("fr-FR")} pts à verser
              </p>
            )}
          </div>
          <div className="flex flex-wrap gap-2">
            <Input
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              placeholder="Joueur ou match…"
              className="w-48"
              aria-label="Filtrer les paris"
            />
            <Button variant="outline" className="gap-1.5" onClick={() => setConfirmResolve(true)} disabled={resolveMutation.isPending}>
              <CheckCheck className="h-4 w-4" />
              Résoudre maintenant
            </Button>
          </div>
        </div>

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
                  <TableHead className="text-right">Action</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {shown.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center text-muted-foreground">
                      {bets.length === 0 ? "Aucun pari en attente." : "Aucun pari ne correspond."}
                    </TableCell>
                  </TableRow>
                )}
                {shown.map((bet) => (
                  <TableRow key={bet.id}>
                    <TableCell className="font-medium">@{bet.username}</TableCell>
                    <TableCell className="text-sm">
                      {bet.selections.map((s, i) => (
                        <span key={i} className="block">
                          <span className="font-medium">{s.label}</span>
                          <span className="text-xs text-muted-foreground"> · {s.matchLabel}</span>
                        </span>
                      ))}
                    </TableCell>
                    <TableCell className="text-right font-mono">{bet.stake} pts</TableCell>
                    <TableCell className="text-right font-mono">{bet.potentialPayout} pts</TableCell>
                    <TableCell className="whitespace-nowrap text-right text-xs text-muted-foreground">
                      {formatRelativeTime(bet.placedAt)}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="outline"
                        size="icon"
                        title="Annuler et rembourser"
                        className="text-destructive hover:text-destructive"
                        onClick={() => setVoiding(bet)}
                      >
                        <Ban className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </CardContent>

      <ReasonDialog
        open={voiding !== null}
        onOpenChange={(open) => !open && setVoiding(null)}
        title={voiding ? `Annuler le pari de @${voiding.username} ?` : ""}
        description={<>Sa mise de {voiding?.stake} pts lui sera remboursée et il sera prévenu. Action immédiate et définitive.</>}
        confirmLabel="Annuler et rembourser"
        destructive
        onConfirm={(reason) => {
          if (voiding) voidMutation.mutate({ id: voiding.id, reason });
          setVoiding(null);
        }}
      />

      <AlertDialog open={confirmResolve} onOpenChange={setConfirmResolve}>
        <AlertDialogContent>
          <AlertDialogTitle>Résoudre les paris en attente ?</AlertDialogTitle>
          <AlertDialogDescription>
            Tous les paris en attente dont les matchs sont terminés seront réglés immédiatement (gains crédités).
            Ça se fait déjà tout seul toutes les 5 minutes : utile seulement juste après une correction de match.
          </AlertDialogDescription>
          <AlertDialogFooter>
            <AlertDialogCancel>Annuler</AlertDialogCancel>
            <AlertDialogAction onClick={() => resolveMutation.mutate()}>Résoudre</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  );
}
