"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, BellRing } from "lucide-react";
import { toast } from "sonner";
import { apiFetch } from "@/lib/api/http";
import type { Match } from "@/lib/types";
import { cn } from "@/lib/utils";

/**
 * Cloche « suivre ce match » : coup d'envoi, fins de quart-temps, fin de
 * match serrée et résultat, dans la cloche et en push (MatchFollowService).
 */
export function FollowMatchButton({ match, className }: { match: Match; className?: string }) {
  const queryClient = useQueryClient();
  const { data: followed } = useQuery({
    queryKey: ["follows"],
    queryFn: () => apiFetch<string[]>("/follows"),
    staleTime: 60 * 1000,
  });
  const isFollowing = !!followed?.includes(match.id);

  const mutation = useMutation({
    mutationFn: (follow: boolean) => apiFetch<void>(`/matches/${match.id}/follow`, { method: follow ? "POST" : "DELETE" }),
    onSuccess: (_, follow) => {
      queryClient.setQueryData<string[]>(["follows"], (ids = []) =>
        follow ? [...ids, match.id] : ids.filter((id) => id !== match.id)
      );
      if (follow) {
        toast.success("Match suivi", {
          description:
            "Coup d'envoi, fins de quart-temps, fin serrée et résultat. Sur téléphone, active les notifications de l'appareil dans Paramètres.",
        });
      }
    },
    onError: (error) => toast.error(error instanceof Error && error.message ? error.message : "Impossible de suivre ce match."),
  });

  if (match.status === "finished" && !isFollowing) return null;
  const Icon = isFollowing ? BellRing : Bell;
  return (
    <button
      type="button"
      onClick={() => mutation.mutate(!isFollowing)}
      disabled={mutation.isPending || match.status === "finished"}
      aria-pressed={isFollowing}
      aria-label={isFollowing ? "Ne plus suivre ce match" : "Suivre ce match"}
      title={isFollowing ? "Ne plus suivre ce match" : "Suivre ce match (notifications)"}
      className={cn(
        "flex h-10 w-10 items-center justify-center rounded-full transition-colors",
        isFollowing ? "glass-accent text-primary" : "glass-inset-quiet text-muted-foreground hover:text-foreground",
        className
      )}
    >
      <Icon className="h-5 w-5" />
    </button>
  );
}
