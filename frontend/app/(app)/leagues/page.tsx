"use client";

import { use, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ChevronRight, Copy, KeyRound, Plus, Share2, Shield, Trophy, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { LeagueCrest } from "@/components/league-crest";
import { PlayerAvatar } from "@/components/player-avatar";
import {
  createLeague,
  fetchLeagueLeaderboard,
  fetchLeagueMembers,
  fetchMyLeagues,
  joinLeague,
  previewLeague,
} from "@/lib/api/leagues";
import { fetchProfile } from "@/lib/api/auth";
import { copyLeagueCode, shareLeagueInvite } from "@/lib/league-invite";
import { League, LeaderboardEntry, LeagueMember, LeaguePreview } from "@/lib/types";

function MemberStack({ members }: { members: LeagueMember[] | undefined }) {
  if (!members?.length) return null;
  const shown = members.slice(0, 5);
  return (
    <span className="flex items-center">
      {shown.map((m, i) => (
        <span key={m.username} className={i > 0 ? "-ml-2" : undefined} title={m.username}>
          <PlayerAvatar
            number={m.avatarNumber}
            position={m.avatarPosition}
            colorway={m.avatarColorway}
            icon={m.avatarIcon}
            size="xs"
          />
        </span>
      ))}
      {members.length > shown.length && (
        <span className="ml-1.5 text-xs text-muted-foreground">+{members.length - shown.length}</span>
      )}
    </span>
  );
}

function LeagueCard({
  league,
  members,
  leaderboard,
  myUsername,
}: {
  league: League;
  members: LeagueMember[] | undefined;
  leaderboard: LeaderboardEntry[] | undefined;
  myUsername: string | undefined;
}) {
  const leader = leaderboard?.[0];
  const me = leaderboard?.find((e) => e.username === myUsername);

  return (
    <Card className="relative overflow-hidden transition-shadow hover:shadow-[inset_0_1px_0_rgba(255,255,255,0.30),inset_0_0_0_1px_rgba(255,122,26,0.30),var(--lift)]">
      <CardContent className="flex flex-col gap-4 pt-6">
        <Link href={`/leagues/${league.id}`} className="flex items-center gap-3">
          <LeagueCrest name={league.name} size={48} />
          <div className="min-w-0 flex-1">
            <p className="flex items-center gap-2 font-heading text-lg font-bold">
              <span className="truncate">{league.name}</span>
              {league.isOwner && (
                <span className="shrink-0 rounded-full bg-paint/10 px-2 py-0.5 text-[10px] font-semibold uppercase text-paint">
                  Créateur
                </span>
              )}
            </p>
            {/* div et non p : les avatars sont des div, interdits dans un p
                (erreur d'hydratation). */}
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Users className="h-3 w-3" />
              {league.memberCount} membre{league.memberCount > 1 ? "s" : ""}
              <MemberStack members={members} />
            </div>
          </div>
          <ChevronRight className="h-5 w-5 shrink-0 text-muted-foreground" />
        </Link>

        <div className="grid grid-cols-2 gap-2 text-sm">
          <div className="glass-inset-quiet rounded-xl px-3 py-2">
            <p className="text-[11px] uppercase tracking-wide text-muted-foreground">Ton rang</p>
            <p className="font-mono font-bold">
              {me ? `#${me.rank}` : "-"}
              <span className="text-xs font-normal text-muted-foreground"> / {league.memberCount}</span>
            </p>
          </div>
          <div className="glass-inset-quiet min-w-0 rounded-xl px-3 py-2">
            <p className="flex items-center gap-1 text-[11px] uppercase tracking-wide text-muted-foreground">
              <Trophy className="h-3 w-3 text-paint" />
              En tête
            </p>
            <p className="truncate font-medium">{leader ? leader.username : "Personne encore"}</p>
          </div>
        </div>

        <div className="flex items-center justify-between gap-2">
          <button
            type="button"
            onClick={() => copyLeagueCode(league.inviteCode)}
            className="glass-inset flex items-center gap-1.5 rounded-xl px-2.5 py-1.5 font-mono text-xs font-bold tracking-widest text-muted-foreground transition-colors hover:text-foreground"
            aria-label={`Copier le code ${league.inviteCode}`}
          >
            {league.inviteCode}
            <Copy className="h-3 w-3" />
          </button>
          <Button variant="outline" size="sm" onClick={() => shareLeagueInvite(league)}>
            <Share2 className="h-3.5 w-3.5" />
            Inviter
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

export default function LeaguesPage({ searchParams }: { searchParams: Promise<{ code?: string }> }) {
  // Lien d'invitation (/leagues?code=XXXXXX) : code pré-rempli, ligue affichée.
  const { code: codeFromLink } = use(searchParams);
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [code, setCode] = useState((codeFromLink ?? "").toUpperCase().slice(0, 6));
  const [preview, setPreview] = useState<LeaguePreview | null>(null);

  const leaguesQuery = useQuery({ queryKey: ["leagues"], queryFn: fetchMyLeagues });
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const leagues = useMemo(() => leaguesQuery.data ?? [], [leaguesQuery.data]);

  // Membres et classement de chaque ligue, pour les cartes. Mêmes clés de
  // cache que la page d'une ligue : l'ouvrir ensuite est instantané.
  const membersQueries = useQueries({
    queries: leagues.map((l) => ({
      queryKey: ["league-members", l.id],
      queryFn: () => fetchLeagueMembers(l.id),
      staleTime: 60 * 1000,
    })),
  });
  const leaderboardQueries = useQueries({
    queries: leagues.map((l) => ({
      queryKey: ["league-leaderboard", l.id],
      queryFn: () => fetchLeagueLeaderboard(l.id),
      staleTime: 60 * 1000,
    })),
  });

  const createMutation = useMutation({
    mutationFn: () => createLeague(name.trim()),
    onSuccess: (league) => {
      queryClient.setQueryData(["leagues"], (prev: League[] | undefined) => [...(prev ?? []), league]);
      setName("");
      toast.success(`Ligue "${league.name}" créée · code ${league.inviteCode}`, {
        action: { label: "Inviter", onClick: () => shareLeagueInvite(league) },
      });
    },
    onError: () => toast.error("Impossible de créer la ligue. Réessaie."),
  });

  const previewMutation = useMutation({
    mutationFn: (inviteCode: string) => previewLeague(inviteCode),
    onSuccess: (result) => setPreview(result),
    onError: () => toast.error("Code d'invitation invalide."),
  });
  const { mutate: showPreview } = previewMutation;

  const joinMutation = useMutation({
    mutationFn: () => joinLeague(code.trim()),
    onSuccess: (league) => {
      queryClient.invalidateQueries({ queryKey: ["leagues"] });
      setCode("");
      setPreview(null);
      toast.success(`Tu as rejoint "${league.name}" !`);
    },
    onError: () => toast.error("Impossible de rejoindre cette ligue. Réessaie."),
  });

  // Arrivée par un lien d'invitation : on affiche directement la ligue, une
  // seule fois. Si l'utilisateur en est déjà membre, rien à confirmer.
  const linkHandled = useRef(false);
  useEffect(() => {
    if (linkHandled.current || !codeFromLink || leaguesQuery.isLoading) return;
    linkHandled.current = true;
    const already = leagues.find((l) => l.inviteCode === codeFromLink.toUpperCase());
    if (already) {
      toast.info(`Tu fais déjà partie de "${already.name}".`);
      return;
    }
    showPreview(codeFromLink.toUpperCase().slice(0, 6));
  }, [codeFromLink, leaguesQuery.isLoading, leagues, showPreview]);

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="flex items-center gap-2.5 font-heading text-2xl font-bold">
          <Shield className="h-7 w-7 text-paint" />
          Ligues
        </h1>
        <p className="mt-1 text-muted-foreground">
          Crée une ligue privée ou rejoins celle de tes potes pour comparer vos pronostics.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Card>
          <CardContent className="flex flex-col gap-3 pt-6">
            <p className="flex items-center gap-1.5 text-xs uppercase tracking-wide text-muted-foreground">
              <Plus className="h-3.5 w-3.5" />
              Créer une ligue
            </p>
            <div className="flex gap-2">
              <Input
                placeholder="Nom de la ligue"
                value={name}
                maxLength={40}
                onChange={(e) => setName(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && name.trim() && createMutation.mutate()}
              />
              {name.trim() && <LeagueCrest name={name} size={40} />}
              <Button onClick={() => createMutation.mutate()} disabled={createMutation.isPending || !name.trim()}>
                Créer
              </Button>
            </div>
          </CardContent>
        </Card>

        <Card className={preview ? "glass-accent" : undefined}>
          <CardContent className="flex flex-col gap-3 pt-6">
            <p className="flex items-center gap-1.5 text-xs uppercase tracking-wide text-muted-foreground">
              <KeyRound className="h-3.5 w-3.5" />
              Rejoindre avec un code
            </p>

            {preview ? (
              <div className="flex flex-col gap-3">
                <div className="flex items-center gap-3">
                  <LeagueCrest name={preview.name} size={44} />
                  <div>
                    <p className="font-heading font-bold text-foreground">{preview.name}</p>
                    <p className="text-xs text-muted-foreground">
                      {preview.memberCount} membre{preview.memberCount > 1 ? "s" : ""} · code{" "}
                      <span className="font-mono font-bold">{code}</span>
                    </p>
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button onClick={() => joinMutation.mutate()} disabled={joinMutation.isPending}>
                    Rejoindre la ligue
                  </Button>
                  <Button variant="outline" onClick={() => setPreview(null)} disabled={joinMutation.isPending}>
                    Annuler
                  </Button>
                </div>
              </div>
            ) : (
              <div className="flex gap-2">
                <Input
                  placeholder="Ex. K7P2QX"
                  value={code}
                  onChange={(e) => setCode(e.target.value.toUpperCase())}
                  onKeyDown={(e) => e.key === "Enter" && code.trim() && previewMutation.mutate(code.trim())}
                  className="font-mono uppercase tracking-widest"
                  maxLength={6}
                  aria-label="Code d'invitation"
                />
                <Button
                  variant="outline"
                  onClick={() => previewMutation.mutate(code.trim())}
                  disabled={previewMutation.isPending || !code.trim()}
                >
                  Rechercher
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {leaguesQuery.isLoading && (
        <div className="grid gap-4 md:grid-cols-2">
          <Skeleton className="h-56 w-full rounded-2xl" />
          <Skeleton className="h-56 w-full rounded-2xl" />
        </div>
      )}

      {leaguesQuery.isError && (
        <Card>
          <CardContent className="py-10 text-center text-destructive">
            Impossible de charger tes ligues. Réessaie plus tard.
          </CardContent>
        </Card>
      )}

      {!leaguesQuery.isLoading && !leaguesQuery.isError && leagues.length === 0 && (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-10 text-center text-muted-foreground">
            <Shield className="h-8 w-8 text-paint" />
            <p className="font-medium text-foreground">Pas encore de ligue</p>
            <p className="max-w-sm text-sm">
              Crées-en une et envoie le lien d&apos;invitation à tes potes, ou rejoins la leur avec leur code.
            </p>
          </CardContent>
        </Card>
      )}

      {leagues.length > 0 && (
        <div className="stagger-children grid gap-4 md:grid-cols-2">
          {leagues.map((league, i) => (
            <LeagueCard
              key={league.id}
              league={league}
              members={membersQueries[i]?.data}
              leaderboard={leaderboardQueries[i]?.data}
              myUsername={profile?.username}
            />
          ))}
        </div>
      )}
    </div>
  );
}
