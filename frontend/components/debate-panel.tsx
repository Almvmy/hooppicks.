"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Check } from "lucide-react";
import { toast } from "sonner";
import { debateShare, voteDebate, type Debate } from "@/lib/api/debate";
import { cn } from "@/lib/utils";

/**
 * La question et ses deux camps, face à face. Les photos seulement si les deux camps
 * en ont une : Jordan sans portrait contre LeBron en photo, le duel paraîtrait truqué.
 * On change de camp tant que le débat est ouvert.
 */
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
  const pictured = !!debate.imageA && !!debate.imageB;
  const shareA = debateShare(debate.votesA ?? 0, debate.votesB ?? 0);
  const shareB = debateShare(debate.votesB ?? 0, debate.votesA ?? 0);
  const sides = [
    { key: "A" as const, label: debate.optionA, image: debate.imageA, share: shareA, tint: "bg-brand/10", number: "text-primary" },
    { key: "B" as const, label: debate.optionB, image: debate.imageB, share: shareB, tint: "bg-paint/15", number: "text-paint" },
  ];

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-3">
        {debate.imageUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={debate.imageUrl} alt="" className="h-14 w-14 shrink-0 rounded-full bg-tint/10 object-cover object-top" />
        )}
        <p className="min-w-0 font-heading text-lg font-bold leading-snug">{debate.question}</p>
      </div>

      <div className="relative grid grid-cols-2 gap-2">
        {sides.map((side) => {
          const mine = debate.myVote === side.key;
          return (
            <button
              key={side.key}
              type="button"
              disabled={!debate.open || mine || vote.isPending}
              aria-pressed={mine}
              onClick={() => vote.mutate(side.key)}
              className={cn(
                "relative flex min-h-28 min-w-0 flex-col items-center justify-end overflow-hidden rounded-2xl px-3 pb-3 text-center transition-shadow",
                side.tint,
                pictured ? "pt-0" : "justify-center pt-3",
                debate.open && !mine && "hover:ring-1 hover:ring-primary/60",
                mine && "ring-2 ring-primary"
              )}
            >
              {pictured && (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={side.image!} alt="" className="h-28 w-full object-contain object-bottom sm:h-40" />
              )}
              {mine && (
                <span className="absolute right-2 top-2 grid h-6 w-6 place-items-center rounded-full bg-primary text-primary-foreground">
                  <Check className="h-3.5 w-3.5" aria-label="Ton camp" />
                </span>
              )}
              <span className="mt-2 max-w-full break-words font-heading text-base font-bold leading-tight sm:text-lg">{side.label}</span>
              {revealed && <span className={cn("mt-1 font-mono text-2xl font-bold tabular-nums", side.number)}>{side.share} %</span>}
            </button>
          );
        })}
        <span
          aria-hidden
          className="pointer-events-none absolute left-1/2 top-1/2 grid h-10 w-10 -translate-x-1/2 -translate-y-1/2 place-items-center rounded-full bg-brand font-heading text-sm font-bold text-brand-foreground ring-4 ring-card"
        >
          VS
        </span>
      </div>

      {revealed && debate.total > 0 && (
        <div className="flex h-2 gap-0.5 overflow-hidden rounded-full" aria-hidden>
          <span className="bg-brand transition-all" style={{ width: `${shareA}%` }} />
          <span className="flex-1 bg-paint" />
        </div>
      )}
      <p className="text-xs text-muted-foreground">
        {debate.total} vote{debate.total > 1 ? "s" : ""}
      </p>
    </div>
  );
}
