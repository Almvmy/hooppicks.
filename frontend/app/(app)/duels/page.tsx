"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Check, Search, Swords, Trophy, X } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { PageHeader } from "@/components/page-header";
import { PlayerAvatar } from "@/components/player-avatar";
import { challengeToDuel, fetchDuels, respondToDuel } from "@/lib/api/duels";
import { fetchLeagueMembers, fetchMyLeagues } from "@/lib/api/leagues";
import { fetchLeaderboard, leaderboardQueryKey } from "@/lib/api/leaderboard";
import { fetchProfile } from "@/lib/api/auth";
import { normalizeForSearch } from "@/lib/help-content";
import { AvatarColorway, AvatarIcon, AvatarPosition, Duel } from "@/lib/types";
import { cn, formatBankrollReset, formatSignedPoints } from "@/lib/utils";

function Opponent({ duel, reverse = false }: { duel: Duel; reverse?: boolean }) {
  return (
    <Link
      href={`/u/${encodeURIComponent(duel.opponentUsername)}`}
      className={cn("flex min-w-0 items-center gap-2.5 hover:underline", reverse && "flex-row-reverse")}
    >
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

interface Candidate {
  username: string;
  avatarNumber: number;
  avatarPosition: AvatarPosition;
  avatarColorway: AvatarColorway;
  avatarIcon: AvatarIcon;
  /** « Les Ballers d'Abidjan », « #4 cette semaine » */
  context: string;
}

const SUGGESTIONS = 6;

/**
 * Défier quelqu'un sans passer par son profil : les membres de ses ligues
 * d'abord (on défie surtout ses potes), puis les joueurs classés cette
 * semaine ; la recherche couvre aussi le classement de la saison.
 */
function ChallengePicker({ busy }: { busy: Set<string> }) {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const leaguesQuery = useQuery({ queryKey: ["leagues"], queryFn: fetchMyLeagues });
  const leagues = useMemo(() => leaguesQuery.data ?? [], [leaguesQuery.data]);
  const members = useQueries({
    queries: leagues.map((l) => ({ queryKey: ["league-members", l.id], queryFn: () => fetchLeagueMembers(l.id), staleTime: 60_000 })),
  });
  const week = useQuery({ queryKey: leaderboardQueryKey("week"), queryFn: () => fetchLeaderboard("week"), staleTime: 2 * 60_000 });
  const season = useQuery({ queryKey: leaderboardQueryKey("season"), queryFn: () => fetchLeaderboard("season"), staleTime: 2 * 60_000 });

  const challenge = useMutation({
    mutationFn: (opponent: string) => challengeToDuel(opponent),
    onSuccess: (_, opponent) => {
      toast.success(`Défi envoyé à @${opponent}.`);
      queryClient.invalidateQueries({ queryKey: ["duels"] });
    },
    onError: (error) => toast.error(error instanceof Error && error.message ? error.message : "Défi impossible."),
  });

  const candidates = useMemo(() => {
    const seen = new Map<string, Candidate>();
    const add = (c: Candidate) => {
      if (c.username === profile?.username || busy.has(c.username) || seen.has(c.username)) return;
      seen.set(c.username, c);
    };
    leagues.forEach((l, i) => members[i]?.data?.forEach((m) => add({ ...m, context: l.name })));
    week.data?.forEach((e) => add({ ...e, context: `#${e.rank} cette semaine` }));
    season.data?.forEach((e) => add({ ...e, context: `#${e.rank} de la saison` }));
    return [...seen.values()];
  }, [leagues, members, week.data, season.data, profile?.username, busy]);

  const q = normalizeForSearch(query.trim());
  const shown = q ? candidates.filter((c) => normalizeForSearch(c.username).includes(q)).slice(0, 10) : candidates.slice(0, SUGGESTIONS);

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-5">
        <h2 className="flex items-center gap-2 font-heading text-base font-bold">
          <Swords className="h-4 w-4 text-primary" />
          Défier quelqu&apos;un
        </h2>
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Chercher un joueur" aria-label="Chercher un joueur" className="pl-9" />
        </div>
        {shown.length === 0 ? (
          <p className="py-2 text-sm text-muted-foreground">{q ? "Aucun joueur ne correspond." : "Personne à défier pour l'instant."}</p>
        ) : (
          <div className="flex flex-col divide-y divide-tint/10">
            {shown.map((c) => (
              <div key={c.username} className="flex items-center gap-3 py-2.5">
                <Link href={`/u/${encodeURIComponent(c.username)}`} className="flex min-w-0 flex-1 items-center gap-2.5 hover:underline">
                  <PlayerAvatar number={c.avatarNumber} position={c.avatarPosition} colorway={c.avatarColorway} icon={c.avatarIcon} size="sm" />
                  <span className="min-w-0">
                    <span className="block truncate font-semibold">@{c.username}</span>
                    <span className="block truncate text-[11px] text-muted-foreground">{c.context}</span>
                  </span>
                </Link>
                <Button
                  size="sm"
                  variant="outline"
                  disabled={challenge.isPending}
                  onClick={() => challenge.mutate(c.username)}
                >
                  Défier
                </Button>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

/** Duel en cours : les deux maillots face à face, l'écart en clair. */
function LiveDuel({ duel, me }: { duel: Duel; me: { avatarNumber: number; avatarPosition: AvatarPosition; avatarColorway: AvatarColorway; avatarIcon: AvatarIcon } | undefined }) {
  const mine = duel.myPoints ?? 0;
  const theirs = duel.opponentPoints ?? 0;
  const gap = Math.abs(mine - theirs);
  return (
    <Card>
      <CardContent className="flex flex-col gap-3">
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            {me && <PlayerAvatar number={me.avatarNumber} position={me.avatarPosition} colorway={me.avatarColorway} icon={me.avatarIcon} size="sm" />}
            <span className="font-semibold">Toi</span>
          </div>
          <span className="font-heading text-xs font-bold uppercase tracking-wide text-muted-foreground">vs</span>
          <Opponent duel={duel} reverse />
        </div>
        <ScoreBar mine={mine} theirs={theirs} />
        <p className="text-xs text-muted-foreground">
          {mine === theirs ? "À égalité pour l'instant." : mine > theirs ? `Tu mènes de ${gap.toLocaleString("fr-FR")} pts.` : `${gap.toLocaleString("fr-FR")} pts à reprendre.`}
        </p>
      </CardContent>
    </Card>
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
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  // Un duel par paire et par semaine : pas de bouton « Défier » pour un adversaire déjà engagé.
  const busy = new Set(duels.filter((d) => d.status === "pending" || d.status === "accepted").map((d) => d.opponentUsername));

  return (
    <div className="flex flex-col gap-6">
      <PageHeader badge="Compétition" title="Duels" subtitle={`Fin de la semaine ${formatBankrollReset()}`} />

      {finished.length > 0 && (
        <div className="grid grid-cols-3 gap-2">
          <div className="glass rounded-2xl px-3 py-2.5">
            <p className="text-[11px] uppercase tracking-wide text-muted-foreground">Victoires</p>
            <p className="font-heading text-2xl font-bold text-success">{wins}</p>
          </div>
          <div className="glass rounded-2xl px-3 py-2.5">
            <p className="text-[11px] uppercase tracking-wide text-muted-foreground">Défaites</p>
            <p className="font-heading text-2xl font-bold text-destructive">{losses}</p>
          </div>
          <div className="glass rounded-2xl px-3 py-2.5">
            <p className="text-[11px] uppercase tracking-wide text-muted-foreground">Nuls</p>
            <p className="font-heading text-2xl font-bold">{finished.length - wins - losses}</p>
          </div>
        </div>
      )}

      {isError && <p className="text-destructive">Impossible de charger tes duels. Réessaie plus tard.</p>}
      {isLoading && <Skeleton className="h-32 w-full rounded-2xl" />}


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
          <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
            {live.map((d) => (
              <LiveDuel key={d.id} duel={d} me={profile} />
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

      {!isLoading && !isError && <ChallengePicker busy={busy} />}

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
