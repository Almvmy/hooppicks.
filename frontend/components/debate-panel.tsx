"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Check } from "lucide-react";
import { toast } from "sonner";
import { debateShare, voteDebate, type Debate } from "@/lib/api/debate";
import { cn } from "@/lib/utils";

/** La question et ses deux camps. On change de camp tant que le débat est ouvert. */
export function DebatePanel({ debate }: { debate: Debate }) {
  const queryClient = useQueryClient();
  const vote = useMutation({
    mutationFn: voteDebate,
    onSuccess: (fresh) => {
      queryClient.setQueryData(["debate", "today"], fresh);
      queryClient.invalidateQueries({ queryKey: ["night-thread"] });
    },
    onError: (e: Error) => toast.error(e.message || "Vote impossible. Réessaie."),
  });
  const revealed = debate.votesA !== null && debate.votesB !== null;
  const sides = [
    { key: "A" as const, label: debate.optionA, votes: debate.votesA ?? 0, other: debate.votesB ?? 0 },
    { key: "B" as const, label: debate.optionB, votes: debate.votesB ?? 0, other: debate.votesA ?? 0 },
  ];

  return (
    <div className="flex flex-col gap-2.5">
      <p className="font-heading text-lg font-bold leading-snug">{debate.question}</p>
      <div className="flex flex-col gap-2">
        {sides.map((side) => {
          const mine = debate.myVote === side.key;
          const share = debateShare(side.votes, side.other);
          return (
            <button
              key={side.key}
              type="button"
              disabled={!debate.open || mine || vote.isPending}
              aria-pressed={mine}
              onClick={() => vote.mutate(side.key)}
              className={cn(
                "glass-inset-quiet relative flex min-h-11 items-center gap-2 overflow-hidden rounded-xl px-3 py-2 text-left text-sm font-semibold transition-colors",
                debate.open && !mine && "hover:text-primary",
                mine && "ring-1 ring-primary"
              )}
            >
              {revealed && (
                <span aria-hidden className={cn("absolute inset-y-0 left-0 transition-all", mine ? "bg-primary/25" : "bg-tint/10")} style={{ width: `${share}%` }} />
              )}
              <span className="relative min-w-0 flex-1 truncate">{side.label}</span>
              {mine && <Check className="relative h-4 w-4 shrink-0 text-primary" aria-label="Ton camp" />}
              {revealed && <span className="relative shrink-0 font-mono text-sm font-bold">{share} %</span>}
            </button>
          );
        })}
      </div>
      <p className="text-xs text-muted-foreground">
        {debate.total} vote{debate.total > 1 ? "s" : ""}
      </p>
    </div>
  );
}
