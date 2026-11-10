"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowDown, ArrowUp, HandCoins, PauseCircle } from "lucide-react";
import { toast } from "sonner";
import { CashOutError, cashOutBet, fetchCashOutQuotes, type CashOutQuote } from "@/lib/api/bets";
import type { PlacedBet } from "@/lib/types";
import { cn, formatSignedPoints } from "@/lib/utils";

/** La valeur d'encaissement des tickets en attente, revue toutes les 20 s (le direct d'ESPN aussi). */
export function useCashOutQuotes(enabled: boolean): Map<string, CashOutQuote> {
  const { data } = useQuery({
    queryKey: ["bets", "cash-out"],
    queryFn: fetchCashOutQuotes,
    enabled,
    refetchInterval: 20_000,
  });
  return new Map((data ?? []).map((q) => [q.betId, q]));
}

/**
 * « Encaisser 142 pts » sur un ticket en attente, avec une confirmation qui dit ce que ça
 * compte au classement. Rien ne revient sur le solde : seul le bénéfice compte, comme un
 * ticket gagnant. Grisé (avec la raison) quand c'est suspendu, absent quand c'est fini.
 */
export function CashOutButton({ bet, quote, compact }: { bet: PlacedBet; quote: CashOutQuote | undefined; compact?: boolean }) {
  const queryClient = useQueryClient();
  const [confirming, setConfirming] = useState(false);
  // Flèche verte ou rouge : la valeur a monté ou baissé depuis le dernier passage.
  const [seen, setSeen] = useState({ value: quote?.value ?? 0, trend: 0 });
  if (quote && seen.value !== quote.value) setSeen({ value: quote.value, trend: Math.sign(quote.value - seen.value) });

  const mutation = useMutation({
    mutationFn: (expected: number) => cashOutBet(bet.id, expected),
    onSuccess: (done) => {
      toast.success(`Ticket encaissé : ${formatSignedPoints(done.potentialPayout - done.stake)} au classement`);
      setConfirming(false);
      queryClient.invalidateQueries({ queryKey: ["bets"] });
      queryClient.invalidateQueries({ queryKey: ["leaderboard"] });
    },
    onError: (e: Error) => {
      toast.error(e.message || "Encaissement impossible. Réessaie.");
      if (e instanceof CashOutError) queryClient.invalidateQueries({ queryKey: ["bets", "cash-out"] });
    },
  });

  if (!quote) return null;
  if (!quote.available) {
    return (
      <p className={cn("flex items-center gap-1.5 text-xs text-muted-foreground", !compact && "self-end")}>
        <PauseCircle className="h-3.5 w-3.5 shrink-0" />
        Encaissement suspendu{quote.suspended ? ` · ${quote.suspended.toLowerCase()}` : ""}
      </p>
    );
  }

  const profit = quote.value - bet.stake;
  const Trend = seen.trend > 0 ? ArrowUp : seen.trend < 0 ? ArrowDown : null;

  if (confirming) {
    return (
      <div className="glass-inset-quiet flex flex-col gap-2 rounded-xl p-3 text-sm">
        <p>
          Encaisser <span className="font-mono font-bold">{quote.value.toLocaleString("fr-FR")} pts</span> maintenant ?{" "}
          <span className={cn("font-semibold", profit >= 0 ? "text-success" : "text-destructive")}>{formatSignedPoints(profit)}</span> au classement,
          le ticket s&apos;arrête là.
        </p>
        <div className="flex gap-2">
          <button
            type="button"
            disabled={mutation.isPending}
            onClick={() => mutation.mutate(quote.value)}
            className="min-h-10 flex-1 rounded-lg bg-brand px-3 font-semibold text-brand-foreground disabled:opacity-60"
          >
            Encaisser
          </button>
          <button type="button" onClick={() => setConfirming(false)} className="glass-inset-quiet min-h-10 flex-1 rounded-lg px-3 font-medium">
            Garder le ticket
          </button>
        </div>
      </div>
    );
  }

  return (
    <button
      type="button"
      onClick={(e) => {
        e.preventDefault();
        setConfirming(true);
      }}
      className={cn(
        "glass-accent flex min-h-10 items-center justify-center gap-2 rounded-xl px-3 text-sm font-semibold",
        !compact && "self-stretch sm:self-end"
      )}
    >
      <HandCoins className="h-4 w-4" />
      Encaisser <span className="font-mono tabular-nums">{quote.value.toLocaleString("fr-FR")} pts</span>
      {Trend && <Trend className={cn("h-3.5 w-3.5", seen.trend > 0 ? "text-success" : "text-destructive")} aria-label={seen.trend > 0 ? "en hausse" : "en baisse"} />}
    </button>
  );
}
