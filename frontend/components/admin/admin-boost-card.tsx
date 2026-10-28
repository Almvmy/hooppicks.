"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Flame } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { chooseBoost, fetchBoostCandidates } from "@/lib/api/admin";
import { cn, formatMatchTime } from "@/lib/utils";

/**
 * Cote boostée de ce soir : le « choc » est choisi tout seul, l'admin peut le
 * remplacer (match plus attendu, derby de la ligue…). Les tickets déjà posés
 * gardent leur cote.
 */
export function AdminBoostCard() {
  const queryClient = useQueryClient();
  const { data, isLoading, isError } = useQuery({ queryKey: ["admin-boost"], queryFn: fetchBoostCandidates });
  const mutation = useMutation({
    mutationFn: chooseBoost,
    onSuccess: (fresh) => {
      queryClient.setQueryData(["admin-boost"], fresh);
      queryClient.invalidateQueries({ queryKey: ["boost"] });
      toast.success("Cote boostée changée.");
    },
    onError: (e) => toast.error(e instanceof Error && e.message ? e.message : "Changement impossible."),
  });

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-6">
        <p className="flex items-center gap-2 text-sm font-medium">
          <Flame className="h-4 w-4 text-primary" />
          Cote boostée de ce soir
        </p>
        {isError && <p className="text-sm text-destructive">Impossible de charger les matchs de ce soir.</p>}
        {!isLoading && !isError && data?.length === 0 && (
          <p className="text-sm text-muted-foreground">Plus de match à venir ce soir.</p>
        )}
        <div className="flex flex-wrap gap-2">
          {data?.map((c) => (
            <Button
              key={c.matchId}
              size="sm"
              variant={c.chosen ? "lit" : "outline"}
              disabled={c.chosen || mutation.isPending}
              onClick={() => mutation.mutate(c.matchId)}
              className={cn(c.chosen && "pointer-events-none")}
            >
              {c.chosen && <Flame className="h-3.5 w-3.5" />}
              {c.label}
              <span className="font-mono text-[11px] opacity-70">{formatMatchTime(new Date(c.date))}</span>
            </Button>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
