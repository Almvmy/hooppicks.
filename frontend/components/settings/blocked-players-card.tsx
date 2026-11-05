"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Ban } from "lucide-react";
import { toast } from "sonner";
import { Card, CardContent } from "@/components/ui/card";
import { fetchBlockedPlayers, unblockPlayer } from "@/lib/api/night-thread";

/** Rien tant que personne n'est bloqué : le blocage se fait depuis le fil. */
export function BlockedPlayersCard() {
  const queryClient = useQueryClient();
  const { data } = useQuery({ queryKey: ["blocked-players"], queryFn: fetchBlockedPlayers });
  const unblock = useMutation({
    mutationFn: unblockPlayer,
    onSuccess: (fresh) => {
      queryClient.setQueryData(["blocked-players"], fresh);
      queryClient.invalidateQueries({ queryKey: ["night-thread"] });
    },
    onError: () => toast.error("Action impossible. Réessaie."),
  });
  if (!data || data.length === 0) return null;

  return (
    <Card>
      <CardContent className="flex flex-col gap-2 pt-6">
        <p className="flex items-center gap-1.5 text-xs uppercase tracking-wide text-muted-foreground">
          <Ban className="h-3.5 w-3.5" />
          Joueurs bloqués
        </p>
        {data.map((p) => (
          <div key={p.username} className="flex items-center justify-between gap-2 text-sm">
            <span className="min-w-0 truncate font-medium">{p.username}</span>
            <button type="button" onClick={() => unblock.mutate(p.username)} className="shrink-0 text-xs font-semibold text-primary hover:underline">
              Débloquer
            </button>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
