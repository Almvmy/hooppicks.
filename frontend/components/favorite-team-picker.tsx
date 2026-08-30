"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { updateProfile } from "@/lib/api/auth";
import { NBA_TEAM_NAMES } from "@/lib/nba-teams";

export function FavoriteTeamPicker({ currentTeam }: { currentTeam: string }) {
  const queryClient = useQueryClient();
  const [team, setTeam] = useState(currentTeam || "");

  const mutation = useMutation({
    mutationFn: () => updateProfile({ favoriteTeam: team }),
    onSuccess: (updated) => {
      queryClient.setQueryData(["profile"], updated);
      toast.success("Équipe favorite mise à jour !");
    },
    onError: () => {
      toast.error("Impossible d'enregistrer. Réessaie.");
    },
  });

  return (
    <div className="flex items-center gap-2">
      <select
        value={team}
        onChange={(e) => setTeam(e.target.value)}
        className="h-8 rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 dark:bg-input/30"
      >
        <option value="">- Aucune -</option>
        {NBA_TEAM_NAMES.map((name) => (
          <option key={name} value={name} className="bg-background">
            {name}
          </option>
        ))}
      </select>
      <Button
        size="sm"
        variant="outline"
        onClick={() => mutation.mutate()}
        disabled={mutation.isPending || team === currentTeam}
      >
        {mutation.isPending ? "..." : "Enregistrer"}
      </Button>
    </div>
  );
}
