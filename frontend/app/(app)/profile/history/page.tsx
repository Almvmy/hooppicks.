"use client";

import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { fetchWalletTransactions } from "@/lib/api/wallet";
import { TransactionType, WalletTransaction } from "@/lib/types";
import { cn } from "@/lib/utils";

const TYPE_LABELS: Record<TransactionType, string> = {
  bet_win: "Pari gagné",
  bet_loss: "Pari perdu",
  bet_placed: "Pari engagé",
  bonus: "Bonus",
};

export default function HistoryPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["wallet-transactions"],
    queryFn: fetchWalletTransactions,
  });

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-bold">Historique des points</h1>
        <p className="mt-1 text-muted-foreground">
          Tous les mouvements de ton solde virtuel.
        </p>
      </div>

      <div className="glass rounded-2xl">
        <Table>
          <TableHeader>
            <TableRow className="border-b-0 shadow-[inset_0_-1px_0_rgba(255,255,255,0.08)] hover:bg-transparent">
              <TableHead>Description</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Date</TableHead>
              <TableHead className="text-right">Montant</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading &&
              Array.from({ length: 4 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={4}>
                    <Skeleton className="h-5 w-full" />
                  </TableCell>
                </TableRow>
              ))}

            {isError && (
              <TableRow>
                <TableCell colSpan={4} className="text-center text-destructive">
                  Impossible de charger l&apos;historique. Réessaie plus tard.
                </TableCell>
              </TableRow>
            )}

            {!isLoading &&
              !isError &&
              data?.map((tx: WalletTransaction) => (
                <TableRow key={tx.id}>
                  <TableCell>{tx.description}</TableCell>
                  <TableCell>
                    <Badge variant="outline">{TYPE_LABELS[tx.type]}</Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {new Date(tx.date).toLocaleDateString("fr-FR", {
                      day: "2-digit",
                      month: "short",
                      year: "numeric",
                    })}
                  </TableCell>
                  <TableCell
                    className={cn(
                      "text-right font-mono",
                      tx.amount >= 0 ? "text-success" : "text-destructive"
                    )}
                  >
                    {tx.amount >= 0 ? "+" : ""}
                    {tx.amount.toLocaleString("fr-FR")} pts
                  </TableCell>
                </TableRow>
              ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}