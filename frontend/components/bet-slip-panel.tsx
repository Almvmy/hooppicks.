"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { X, Ticket as TicketIcon } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useBetSlip } from "@/components/bet-slip-provider";
import { fetchWallet } from "@/lib/api/wallet";
import { placeBet } from "@/lib/api/bets";
import type { BetSelection } from "@/lib/types";

export function BetSlipPanel() {
  const { selections, stake, setStake, toggleSelection, clear, totalOdds, potentialPayout } =
    useBetSlip();
  const queryClient = useQueryClient();

  const { data: wallet } = useQuery({ queryKey: ["wallet"], queryFn: fetchWallet });

    const mutation = useMutation({
    mutationFn: ({ selections, stake }: { selections: BetSelection[]; stake: number }) =>
      placeBet(selections, stake),
    onSuccess: () => {
      toast.success("Ticket validé !");
      clear();
      queryClient.invalidateQueries({ queryKey: ["bets"] });
      queryClient.invalidateQueries({ queryKey: ["wallet"] });
    },
    onError: () => {
      toast.error("Impossible de valider le ticket. Réessaie.");
    },
  });

  if (selections.length === 0) return null;

  const balance = wallet?.balance ?? 0;
  const isStakeValid = stake > 0 && stake <= balance;

  function handleSubmit() {
    if (!isStakeValid) return;
    mutation.mutate({ selections, stake });
  }

  return (
    <div className="fixed bottom-20 left-4 right-4 z-50 w-auto overflow-hidden rounded-lg border border-white/10 bg-card/70 shadow-2xl shadow-black/40 backdrop-blur-xl sm:bottom-4 sm:left-auto sm:w-80">
      {/* Bordure haute lumineuse orange → cyan, signature visuelle du BetSlip */}
      <div className="h-[3px] w-full bg-gradient-to-r from-primary via-live to-primary" />

      <div className="flex items-center justify-between border-b border-white/10 px-4 py-3">
        <div className="flex items-center gap-2 font-heading font-bold">
          <TicketIcon className="h-4 w-4 text-primary" />
          Ticket ({selections.length})
        </div>
        <button onClick={clear} aria-label="Vider le ticket">
          <X className="h-4 w-4 text-muted-foreground hover:text-foreground" />
        </button>
      </div>

      <div className="flex max-h-56 flex-col gap-2 overflow-y-auto p-4">
        {selections.map((s) => (
          <div
            key={s.id}
            className="flex items-center justify-between rounded-md border border-white/5 bg-secondary/40 px-3 py-2 text-sm"
          >
            <div>
              <p className="text-xs text-muted-foreground">{s.matchLabel}</p>
              <p className="font-medium">{s.label}</p>
            </div>
            <div className="flex items-center gap-2">
              <span className="font-mono font-bold text-primary">{s.odds.toFixed(2)}</span>
              <button onClick={() => toggleSelection(s)} aria-label="Retirer">
                <X className="h-3.5 w-3.5 text-muted-foreground hover:text-destructive" />
              </button>
            </div>
          </div>
        ))}
      </div>

      <div className="flex flex-col gap-3 border-t border-white/10 p-4">
        <div className="flex items-center justify-between text-sm">
          <span className="text-muted-foreground">Cote totale</span>
          <span className="font-mono font-bold">{totalOdds.toFixed(2)}</span>
        </div>

        <Input
          type="number"
          min={1}
          placeholder="Mise en points"
          value={stake || ""}
          onChange={(e) => setStake(Number(e.target.value))}
        />

        <div className="flex items-center justify-between text-sm">
          <span className="text-muted-foreground">Gain potentiel</span>
          <span className="font-mono font-bold text-success">
            {potentialPayout.toLocaleString("fr-FR")} pts
          </span>
        </div>

        {stake > balance && (
          <p className="text-xs text-destructive">
            Solde insuffisant ({balance.toLocaleString("fr-FR")} pts disponibles).
          </p>
        )}

        <Button
          onClick={handleSubmit}
          disabled={!isStakeValid || mutation.isPending}
          className="w-full"
        >
          {mutation.isPending ? "Validation..." : "Valider le ticket"}
        </Button>
      </div>
    </div>
  );
}