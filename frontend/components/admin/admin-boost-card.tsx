"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Flame, Plus } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { chooseBoost, fetchBoostCandidates, toggleExtraBoost, type BoostCandidate } from "@/lib/api/admin";
import { formatMatchTime } from "@/lib/utils";

/**
 * Cotes boostées de ce soir. Le « choc » est choisi tout seul, l'admin peut le
 * remplacer, et ajouter d'autres matchs pour une soirée spéciale (Noël, phase
 * finale de la Coupe…). Les tickets déjà posés gardent leur cote.
 */
export function AdminBoostCard() {
  const queryClient = useQueryClient();
  const { data, isLoading, isError } = useQuery({ queryKey: ["admin-boost"], queryFn: fetchBoostCandidates });
  const done = (fresh: BoostCandidate[]) => {
    queryClient.setQueryData(["admin-boost"], fresh);
    queryClient.invalidateQueries({ queryKey: ["boost"] });
    toast.success("Cotes boostées mises à jour.");
  };
  const fail = (e: Error) => toast.error(e.message || "Changement impossible.");
  const choose = useMutation({ mutationFn: chooseBoost, onSuccess: done, onError: fail });
  const toggle = useMutation({ mutationFn: toggleExtraBoost, onSuccess: done, onError: fail });
  const pending = choose.isPending || toggle.isPending;

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-6">
        <p className="flex items-center gap-2 text-sm font-medium">
          <Flame className="h-4 w-4 text-primary" />
          Cotes boostées de ce soir
        </p>
        {isError && <p className="text-sm text-destructive">Impossible de charger les matchs de ce soir.</p>}
        {!isLoading && !isError && data?.length === 0 && (
          <p className="text-sm text-muted-foreground">Plus de match à venir ce soir.</p>
        )}
        <div className="flex flex-col divide-y divide-tint/10">
          {data?.map((c) => (
            <div key={c.matchId} className="flex items-center gap-2 py-2">
              <span className="min-w-0 flex-1 truncate text-sm font-medium">{c.label}</span>
              <span className="shrink-0 font-mono text-xs text-muted-foreground">{formatMatchTime(new Date(c.date))}</span>
              <Button size="sm" variant={c.chosen ? "lit" : "outline"} disabled={c.chosen || pending} onClick={() => choose.mutate(c.matchId)}>
                {c.chosen && <Flame className="h-3.5 w-3.5" />}
                Choc
              </Button>
              <Button
                size="sm"
                variant={c.extra ? "lit" : "outline"}
                disabled={c.chosen || pending}
                aria-pressed={c.extra}
                onClick={() => toggle.mutate(c.matchId)}
              >
                <Plus className="h-3.5 w-3.5" />
                En plus
              </Button>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
