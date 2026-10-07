"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronDown, X, Ticket as TicketIcon } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { MAX_SELECTIONS, MIN_STAKE, useBetSlip } from "@/components/bet-slip-provider";
import { fetchWallet } from "@/lib/api/wallet";
import { placeBet } from "@/lib/api/bets";
import type { BetSelection } from "@/lib/types";
import { cn, formatBankrollReset, WEEKLY_BANKROLL, formatOdds } from "@/lib/utils";
import { SelectionTeamLogo, useMatchesById } from "@/components/selection-team-logo";

export function BetSlipPanel() {
  const { selections, stake, setStake, toggleSelection, clear, totalOdds, potentialPayout } =
    useBetSlip();
  const queryClient = useQueryClient();

  const { data: wallet } = useQuery({ queryKey: ["wallet"], queryFn: fetchWallet });
  const matchesById = useMatchesById();

  // Mobile uniquement : réduit par défaut en pastille dans le coin, déplié
  // seulement quand on la touche. Avant, il se dépliait à chaque sélection
  // et masquait la moitié de l'écran pendant qu'on composait un combiné.
  // Déplié, il prend toute la largeur : saisie de la mise + clavier, une
  // carte « dans le coin » ferait de toute façon ~90 % d'un écran de 375px.
  // Chaque sélection ajoutée fait rebondir la pastille (clé qui change) :
  // ajusté pendant le rendu, pas dans un effet.
  const [expanded, setExpanded] = useState(false);
  const [previousCount, setPreviousCount] = useState(selections.length);
  const [bump, setBump] = useState(0);
  if (selections.length !== previousCount) {
    setPreviousCount(selections.length);
    if (selections.length > previousCount) setBump((b) => b + 1);
  }

    const mutation = useMutation({
    mutationFn: ({ selections, stake }: { selections: BetSelection[]; stake: number }) =>
      placeBet(selections, stake),
    onSuccess: () => {
      toast.success("Ticket validé !");
      clear();
      queryClient.invalidateQueries({ queryKey: ["bets"] });
      queryClient.invalidateQueries({ queryKey: ["wallet"] });
    },
    onError: (error) => {
      // Le serveur explique le refus en clair (match commencé, solde…) :
      // on l'affiche plutôt qu'un message générique.
      const message = error instanceof Error ? error.message : "";
      const readable = message && !message.startsWith("{") && !message.startsWith("Erreur API");
      toast.error(readable ? message : "Impossible de valider le ticket. Réessaie.");
    },
  });

  if (selections.length === 0) return null;

  const balance = wallet?.balance ?? 0;
  const isStakeValid = stake >= MIN_STAKE && stake <= balance;

  function handleSubmit() {
    if (!isStakeValid) return;
    mutation.mutate({ selections, stake });
  }

  return (
    <>
      {expanded && (
        // Toucher à côté referme le ticket, comme une feuille de bas d'écran.
        <div
          aria-hidden
          onClick={() => setExpanded(false)}
          className="fixed inset-0 z-50 bg-black/30 backdrop-blur-[2px] md:hidden"
        />
      )}
      {!expanded && (
        <button
          key={bump}
          type="button"
          onClick={() => setExpanded(true)}
          className="slip-pop fixed right-4 z-50 flex items-center gap-2 rounded-full bg-brand px-4 py-2.5 text-sm font-semibold text-brand-foreground shadow-[var(--lift-strong)] md:hidden"
          // Juste au-dessus de la bottom nav (14px + ~68px de haut + marge).
          style={{ bottom: "calc(env(safe-area-inset-bottom, 0px) + 96px)" }}
          aria-label={`Ouvrir le ticket (${selections.length} sélection${selections.length > 1 ? "s" : ""})`}
        >
          <TicketIcon className="h-4 w-4" />
          Ticket ({selections.length})
          <span className="font-mono">{formatOdds(totalOdds)}</span>
        </button>
      )}
      <div
        className={cn(
          "glass island fixed inset-x-0 bottom-0 z-50 overflow-hidden rounded-t-2xl pb-[env(safe-area-inset-bottom)]",
          "md:inset-x-auto md:bottom-4 md:right-4 md:w-80 md:rounded-2xl md:pb-0",
          !expanded && "hidden md:block"
        )}
      >
        {/* Bordure haute lumineuse orange → cyan, signature visuelle du BetSlip */}
        <div className="h-[3px] w-full bg-gradient-to-r from-primary via-live to-primary" />

        <div className="flex items-center justify-between px-4 py-3 shadow-[inset_0_-1px_0_var(--hairline)]">
          <div className="flex items-center gap-2 font-heading font-bold">
            <TicketIcon className="h-4 w-4 text-primary" />
            Ticket ({selections.length}
            <span className="font-normal text-muted-foreground">/{MAX_SELECTIONS}</span>)
          </div>
          <div className="flex items-center gap-3">
            <button onClick={() => setExpanded(false)} aria-label="Réduire le ticket" className="md:hidden">
              <ChevronDown className="h-4 w-4 text-muted-foreground hover:text-foreground" />
            </button>
            <button onClick={clear} aria-label="Vider le ticket">
              <X className="h-4 w-4 text-muted-foreground hover:text-foreground" />
            </button>
          </div>
        </div>

        <div className="flex max-h-56 flex-col gap-2 overflow-y-auto p-4">
          {selections.map((s) => (
            <div
              key={s.id}
              className="glass-inset-quiet flex items-center justify-between rounded-xl px-3 py-2 text-sm"
            >
              <div className="flex min-w-0 items-center gap-2.5">
                <SelectionTeamLogo selection={s} matchesById={matchesById} size={28} />
                <div className="min-w-0">
                  <p className="truncate text-xs text-muted-foreground">{s.matchLabel}</p>
                  <p className="font-medium">{s.label}</p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <span className="font-mono font-bold text-primary">{formatOdds(s.odds)}</span>
                <button onClick={() => toggleSelection(s)} aria-label="Retirer">
                  <X className="h-3.5 w-3.5 text-muted-foreground hover:text-destructive" />
                </button>
              </div>
            </div>
          ))}
        </div>

        <div className="flex flex-col gap-3 p-4 shadow-[inset_0_1px_0_var(--hairline)]">
          <div className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground">Cote totale</span>
            <span className="font-mono font-bold">{formatOdds(totalOdds)}</span>
          </div>

          <Input
            type="number"
            min={MIN_STAKE}
            placeholder={`Mise en points (${MIN_STAKE} minimum)`}
            value={stake || ""}
            onChange={(e) => setStake(Number(e.target.value))}
          />

          <div className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground">Gain potentiel</span>
            <span className="font-mono font-bold text-success">
              {potentialPayout.toLocaleString("fr-FR")} pts
            </span>
          </div>

          {/* Le solde vit ici plutôt qu'en haut de l'écran : c'est au moment de
              miser qu'on en a besoin. En haut, les points de classement. */}
          <p className={cn("text-xs", stake > balance ? "text-destructive" : "text-muted-foreground")}>
            {stake > 0 && stake < MIN_STAKE && <span className="block text-destructive">Mise minimum : {MIN_STAKE} pts.</span>}
            {stake > balance ? "Solde insuffisant : " : "Solde de la semaine : "}
            <span className="font-mono font-semibold">{balance.toLocaleString("fr-FR")} pts</span>
            {" "}· repart à {WEEKLY_BANKROLL.toLocaleString("fr-FR")} {formatBankrollReset()}
          </p>

          <Button
            variant="lit"
            onClick={handleSubmit}
            disabled={!isStakeValid || mutation.isPending}
            className="w-full"
          >
            {mutation.isPending ? "Validation..." : "Valider le ticket"}
          </Button>
        </div>
      </div>
    </>
  );
}