"use client";

import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Check, Swords, Trophy, X } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { PlayerAvatar } from "@/components/player-avatar";
import { fetchDuels, respondToDuel } from "@/lib/api/duels";
import { Duel } from "@/lib/types";
import { cn, formatBankrollReset, formatSignedPoints } from "@/lib/utils";

function Opponent({ duel }: { duel: Duel }) {
  return (
    <Link href={`/u/${encodeURIComponent(duel.opponentUsername)}`} className="flex min-w-0 items-center gap-2.5 hover:underline">
      <PlayerAvatar
        number={duel.opponentAvatarNumber}
        position={duel.opponentAvatarPosition}
        colorway={duel.opponentAvatarColorway}
        icon={duel.opponentAvatarIcon}
        size="sm"
      />
      <span className="truncate font-semibold">@{duel.opponentUsername}</span>
    </Link>
  );
}

/** Barre « toi / lui » : la part de chacun dans le total des points. */
function ScoreBar({ mine, theirs }: { mine: number; theirs: number }) {
  const total = mine + theirs;
  const share = total === 0 ? 50 : Math.round((mine / total) * 100);
  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex items-baseline justify-between font-mono text-sm font-bold">
        <span className={cn(mine >= theirs && total > 0 && "text-primary")}>Toi {formatSignedPoints(mine)}</span>
        <span className={cn(theirs > mine && "text-primary")}>{formatSignedPoints(theirs)}</span>
      </div>
      <div className="flex h-2 overflow-hidden rounded-full bg-tint/10" aria-hidden>
        <div className="bg-primary transition-all" style={{ width: `${share}%` }} />
      </div>
    </div>
  );
}

const RESULT = {
  won: { label: "Victoire", className: "text-success" },
  lost: { label: "Défaite", className: "text-destructive" },
  tie: { label: "Égalité", className: "text-muted-foreground" },
} as const;

export default function DuelsPage() {
  const queryClient = useQueryClient();
  const { data, isLoading, isError } = useQuery({ queryKey: ["duels"], queryFn: fetchDuels, refetchInterval: 60_000 });
  const mutation = useMutation({
    mutationFn: ({ id, action }: { id: string; action: "accept" | "decline" | "cancel" }) => respondToDuel(id, action),
    onSuccess: (_, { action }) => {
      toast.success(action === "accept" ? "Duel accepté, que le meilleur gagne !" : action === "decline" ? "Défi décliné." : "Défi retiré.");
      queryClient.invalidateQueries({ queryKey: ["duels"] });
    },
    onError: (error) => toast.error(error instanceof Error && error.message ? error.message : "Action impossible."),
  });

  const duels = data ?? [];
  const incoming = duels.filter((d) => d.status === "pending" && !d.iChallenged);
  const outgoing = duels.filter((d) => d.status === "pending" && d.iChallenged);
  const live = duels.filter((d) => d.status === "accepted");
  const finished = duels.filter((d) => d.status === "finished");
  const wins = finished.filter((d) => d.result === "won").length;
  const losses = finished.filter((d) => d.result === "lost").length;

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="flex items-center gap-2.5 font-heading text-2xl font-bold">
          <Swords className="h-6 w-6 text-primary" />
          Duels
        </h1>
        <p className="mt-1 text-muted-foreground">
          Défie un joueur sur la semaine : celui qui a le plus de points lundi 12h GMT gagne. Fin de la semaine{" "}
          {formatBankrollReset()}.
        </p>
        {finished.length > 0 && (
          <p className="mt-2 font-mono text-sm">
            Bilan : <span className="text-success">{wins} V</span> · <span className="text-destructive">{losses} D</span>
            {finished.length - wins - losses > 0 && ` · ${finished.length - wins - losses} N`}
          </p>
        )}
      </div>

      {isError && <p className="text-destructive">Impossible de charger tes duels. Réessaie plus tard.</p>}
      {isLoading && <Skeleton className="h-32 w-full rounded-2xl" />}

      {!isLoading && !isError && duels.length === 0 && (
        <Card>
          <CardContent className="flex flex-col items-center gap-3 py-8 text-center">
            <Swords className="h-8 w-8 text-muted-foreground" />
            <p className="max-w-sm text-sm text-muted-foreground">
              Aucun duel pour l&apos;instant. Ouvre le profil d&apos;un joueur (depuis le classement ou une ligue) et appuie
              sur « Défier ».
            </p>
            <Link href="/leaderboard">
              <Button variant="outline" size="sm">Voir le classement</Button>
            </Link>
          </CardContent>
        </Card>
      )}

      {incoming.length > 0 && (
        <section className="flex flex-col gap-3">
          <h2 className="font-heading text-sm font-bold uppercase tracking-wide text-primary">Défis reçus</h2>
          {incoming.map((d) => (
            <Card key={d.id}>
              <CardContent className="flex flex-wrap items-center justify-between gap-3">
                <Opponent duel={d} />
                <div className="flex gap-2">
                  <Button size="sm" variant="lit" disabled={mutation.isPending} onClick={() => mutation.mutate({ id: d.id, action: "accept" })}>
                    <Check className="h-4 w-4" />
                    Accepter
                  </Button>
                  <Button size="sm" variant="ghost" disabled={mutation.isPending} onClick={() => mutation.mutate({ id: d.id, action: "decline" })}>
                    Décliner
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </section>
      )}

      {live.length > 0 && (
        <section className="flex flex-col gap-3">
          <h2 className="font-heading text-sm font-bold uppercase tracking-wide text-muted-foreground">Cette semaine</h2>
          <div className="grid gap-3 lg:grid-cols-2">
            {live.map((d) => (
              <Card key={d.id}>
                <CardContent className="flex flex-col gap-3">
                  <Opponent duel={d} />
                  <ScoreBar mine={d.myPoints ?? 0} theirs={d.opponentPoints ?? 0} />
                </CardContent>
              </Card>
            ))}
          </div>
        </section>
      )}

      {outgoing.length > 0 && (
        <section className="flex flex-col gap-3">
          <h2 className="font-heading text-sm font-bold uppercase tracking-wide text-muted-foreground">En attente de réponse</h2>
          {outgoing.map((d) => (
            <Card key={d.id} size="sm">
              <CardContent className="flex items-center justify-between gap-3">
                <Opponent duel={d} />
                <Button size="sm" variant="ghost" disabled={mutation.isPending} onClick={() => mutation.mutate({ id: d.id, action: "cancel" })}>
                  <X className="h-4 w-4" />
                  Retirer
                </Button>
              </CardContent>
            </Card>
          ))}
        </section>
      )}

      {finished.length > 0 && (
        <section className="flex flex-col gap-3">
          <h2 className="font-heading text-sm font-bold uppercase tracking-wide text-muted-foreground">Historique</h2>
          <Card>
            <CardContent className="divide-y divide-tint/10">
              {finished.map((d) => {
                const r = RESULT[d.result ?? "tie"];
                return (
                  <div key={d.id} className="flex items-center justify-between gap-3 py-2.5">
                    <Opponent duel={d} />
                    <div className="flex shrink-0 items-center gap-3">
                      <span className="font-mono text-sm text-muted-foreground">
                        {d.myPoints ?? 0} – {d.opponentPoints ?? 0}
                      </span>
                      <span className={cn("flex items-center gap-1 text-sm font-semibold", r.className)}>
                        {d.result === "won" && <Trophy className="h-3.5 w-3.5" />}
                        {r.label}
                      </span>
                    </div>
                  </div>
                );
              })}
            </CardContent>
          </Card>
        </section>
      )}
    </div>
  );
}
