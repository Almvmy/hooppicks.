"use client";

import { Layers, Plus } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useBetSlip } from "@/components/bet-slip-provider";
import { useBoost } from "@/lib/boost";
import { suggestParlays } from "@/lib/suggested-parlays";
import type { Match } from "@/lib/types";
import { formatOdds } from "@/lib/utils";

/** Combinés du jour prêts à jouer : une touche les met dans le ticket. */
export function SuggestedParlays({ matches }: { matches: Match[] | undefined }) {
  const boost = useBoost();
  const { selections: inSlip, toggleSelection } = useBetSlip();
  const parlays = suggestParlays(matches, boost);
  if (parlays.length === 0) return null;

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-5">
        <h2 className="flex items-center gap-2 font-heading text-base font-bold">
          <Layers className="h-4 w-4 text-paint" />
          Combinés du jour
        </h2>
        <div className="grid gap-3 sm:grid-cols-2">
          {parlays.map((parlay) => {
            const allIn = parlay.selections.every((s) => inSlip.some((x) => x.id === s.id));
            return (
              <div key={parlay.key} className="glass-inset-quiet flex flex-col gap-2 rounded-xl p-3">
                <div className="flex items-baseline justify-between gap-2">
                  <p className="font-heading font-bold">{parlay.title}</p>
                  <p className="font-mono text-sm font-bold text-primary">{formatOdds(parlay.totalOdds)}</p>
                </div>
                <p className="text-xs text-muted-foreground">{parlay.description}</p>
                <ul className="flex flex-col gap-0.5 text-sm">
                  {parlay.selections.map((s) => (
                    <li key={s.id} className="flex justify-between gap-2">
                      <span className="truncate">{s.label}</span>
                      <span className="font-mono text-xs text-muted-foreground">{formatOdds(s.odds)}</span>
                    </li>
                  ))}
                </ul>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={allIn}
                  // Ajoute seulement ce qui manque : toggleSelection retirerait
                  // une sélection déjà présente.
                  onClick={() =>
                    parlay.selections.filter((s) => !inSlip.some((x) => x.id === s.id)).forEach(toggleSelection)
                  }
                >
                  <Plus className="h-3.5 w-3.5" />
                  {allIn ? "Dans ton ticket" : "Ajouter au ticket"}
                </Button>
              </div>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
}
