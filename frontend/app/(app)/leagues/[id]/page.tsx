"use client";

import { use, useEffect, useState } from "react";
import type { LeaderboardPeriod } from "@/lib/types";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ArrowLeft, Copy, LogOut, Share2, Ticket, TrendingUp, Trophy, UserPlus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { LeaderboardTable } from "@/components/leaderboard-table";
import { PlayerAvatar } from "@/components/player-avatar";
import { LeagueCrest } from "@/components/league-crest";
import { copyLeagueCode, shareLeagueInvite } from "@/lib/league-invite";
import {
  fetchLeagueActivity,
  fetchLeagueLeaderboard,
  fetchLeagueMembers,
  fetchMyLeagues,
  leaveLeague,
  reactToActivity,
} from "@/lib/api/leagues";
import { fetchProfile } from "@/lib/api/auth";
import { formatRelativeTime, formatSignedPoints, cn } from "@/lib/utils";

// Doit matcher ALLOWED_EMOJIS côté backend (LeagueService) : pas de sélecteur
// libre, un petit vocabulaire partagé suffit pour ce genre de réaction.
const REACTION_EMOJIS = ["👍", "🔥", "👎"];

const LEAGUE_PERIODS: { value: LeaderboardPeriod; label: string }[] = [
  { value: "season", label: "Saison" },
  { value: "month", label: "Ce mois-ci" },
  { value: "week", label: "Cette semaine" },
];

export default function LeagueDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();
  const queryClient = useQueryClient();
  const [confirmingLeave, setConfirmingLeave] = useState(false);

  const leaguesQuery = useQuery({ queryKey: ["leagues"], queryFn: fetchMyLeagues });
  const league = leaguesQuery.data?.find((l) => l.id === id);

  // La semaine relance la course entre amis chaque lundi, quand l'écart
  // de la saison est devenu trop grand pour être comblé.
  const [period, setPeriod] = useState<LeaderboardPeriod>("week");
  const leaderboardQuery = useQuery({
    queryKey: ["league-leaderboard", id, period],
    queryFn: () => fetchLeagueLeaderboard(id, period),
  });

  const membersQuery = useQuery({
    queryKey: ["league-members", id],
    queryFn: () => fetchLeagueMembers(id),
  });

  const activityQuery = useQuery({
    queryKey: ["league-activity", id],
    queryFn: () => fetchLeagueActivity(id),
  });

  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const leaderboard = leaderboardQuery.data ?? [];
  const myIndex = leaderboard.findIndex((e) => e.username === profile?.username);
  const me = myIndex >= 0 ? leaderboard[myIndex] : undefined;
  const above = me ? leaderboard.slice(0, myIndex).reverse().find((e) => e.points > me.points) : undefined;

  const reactMutation = useMutation({
    mutationFn: ({ targetType, targetId, emoji }: { targetType: string; targetId: string; emoji: string }) =>
      reactToActivity(id, targetType, targetId, emoji),
    onSuccess: (freshActivity) => {
      queryClient.setQueryData(["league-activity", id], freshActivity);
    },
    onError: () => toast.error("Impossible d'envoyer la réaction. Réessaie."),
  });

  const leaveMutation = useMutation({
    mutationFn: () => leaveLeague(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["leagues"] });
      toast.success("Tu as quitté la ligue.");
      router.push("/leagues");
    },
    onError: () => toast.error("Impossible de quitter la ligue. Réessaie."),
  });

  useEffect(() => {
    if (!confirmingLeave) return;
    const timeout = setTimeout(() => setConfirmingLeave(false), 4000);
    return () => clearTimeout(timeout);
  }, [confirmingLeave]);

  return (
    <div className="flex flex-col gap-6">
      <Link
        href="/leagues"
        className="flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" />
        Mes ligues
      </Link>

      <Card className="relative overflow-hidden">
        <CardContent className="flex flex-wrap items-center justify-between gap-4 pt-6">
          <div className="flex min-w-0 items-center gap-4">
            <LeagueCrest name={league?.name ?? "Ligue"} size={64} />
            <div className="min-w-0">
              <h1 className="truncate font-heading text-2xl font-bold">{league?.name ?? "Ligue"}</h1>
              {league && (
                <p className="mt-1 flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
                  {league.memberCount} membre{league.memberCount > 1 ? "s" : ""}
                  <span aria-hidden>·</span>
                  <button
                    type="button"
                    onClick={() => copyLeagueCode(league.inviteCode)}
                    className="glass-inset flex items-center gap-1.5 rounded-lg px-2 py-0.5 font-mono text-xs font-bold tracking-widest text-foreground"
                    aria-label={`Copier le code ${league.inviteCode}`}
                  >
                    {league.inviteCode}
                    <Copy className="h-3 w-3" />
                  </button>
                </p>
              )}
            </div>
          </div>

          <div className="flex shrink-0 gap-2">
            {league && (
              <Button size="sm" onClick={() => shareLeagueInvite(league)}>
                <Share2 className="h-3.5 w-3.5" />
                Inviter
              </Button>
            )}
            <Button
              variant={confirmingLeave ? "destructive" : "outline"}
              size="sm"
              className="gap-1.5"
              onClick={() => (confirmingLeave ? leaveMutation.mutate() : setConfirmingLeave(true))}
              disabled={leaveMutation.isPending}
            >
              <LogOut className="h-3.5 w-3.5" />
              {confirmingLeave ? "Confirmer ?" : "Quitter"}
            </Button>
          </div>
        </CardContent>
      </Card>

      {me && (
        <div className="grid gap-3 sm:grid-cols-3">
          <div className="glass-accent rounded-2xl px-4 py-3">
            <p className="text-[11px] uppercase tracking-wide text-muted-foreground">Ta position</p>
            <p className="font-heading text-2xl font-bold">
              #{me.rank}
              <span className="text-sm font-normal text-muted-foreground"> / {leaderboard.length}</span>
            </p>
          </div>
          <div className="glass rounded-2xl px-4 py-3">
            <p className="text-[11px] uppercase tracking-wide text-muted-foreground">Tes points</p>
            <p className="font-mono text-2xl font-bold">{formatSignedPoints(me.points, false)}</p>
          </div>
          <div className="glass rounded-2xl px-4 py-3">
            <p className="flex items-center gap-1 text-[11px] uppercase tracking-wide text-muted-foreground">
              <TrendingUp className="h-3 w-3" />
              Objectif
            </p>
            <p className="text-sm font-medium">
              {me.rank === 1
                ? "Tu mènes la ligue."
                : above
                  ? `${(above.points - me.points).toLocaleString("fr-FR")} pts pour doubler ${above.username}`
                  : "À égalité avec le joueur devant toi."}
            </p>
          </div>
        </div>
      )}

      <div className="grid gap-4 lg:grid-cols-[1fr_260px]">
        <div className="glass relative overflow-hidden rounded-2xl">
          <div className="flex gap-2 overflow-x-auto px-4 pt-4" role="group" aria-label="Période du classement">
            {LEAGUE_PERIODS.map((p) => (
              <button
                key={p.value}
                type="button"
                aria-pressed={period === p.value}
                onClick={() => setPeriod(p.value)}
                className={cn(
                  "shrink-0 rounded-full px-3 py-1.5 text-sm transition-colors",
                  period === p.value ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
                )}
              >
                {p.label}
              </button>
            ))}
          </div>
          <LeaderboardTable
            entries={leaderboardQuery.data}
            isLoading={leaderboardQuery.isLoading}
            isError={leaderboardQuery.isError}
            emptyMessage={
              period === "week"
                ? "Aucun ticket réglé dans la ligue cette semaine."
                : period === "month"
                  ? "Aucun ticket réglé dans la ligue ce mois-ci."
                  : "Aucun pari résolu dans cette ligue pour l'instant."
            }
            currentUsername={profile?.username}
          />
        </div>

        <Card>
          <CardContent className="flex flex-col gap-3 pt-6">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">
              Membres ({membersQuery.data?.length ?? "…"})
            </p>

            {membersQuery.isLoading && (
              <div className="flex flex-col gap-2">
                <Skeleton className="h-5 w-full" />
                <Skeleton className="h-5 w-full" />
                <Skeleton className="h-5 w-full" />
              </div>
            )}

            {membersQuery.isError && (
              <p className="text-sm text-destructive">Impossible de charger les membres.</p>
            )}

            {membersQuery.data?.map((member) => (
              <div key={member.username} className="flex items-center justify-between text-sm">
                <Link
                  href={`/u/${encodeURIComponent(member.username)}`}
                  className="flex min-w-0 items-center gap-2 truncate font-medium hover:underline"
                >
                  <PlayerAvatar
                    number={member.avatarNumber}
                    position={member.avatarPosition}
                    colorway={member.avatarColorway}
                    icon={member.avatarIcon}
                    size="xs"
                  />
                  {member.username}
                </Link>
                {member.isOwner && (
                  <span className="shrink-0 rounded-full bg-paint/10 px-2 py-0.5 text-[10px] font-semibold uppercase text-paint">
                    Créateur
                  </span>
                )}
              </div>
            ))}
          </CardContent>
        </Card>

        <Card>
          <CardContent className="flex flex-col gap-3 pt-6">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">
              Activité récente
            </p>

            {activityQuery.isLoading && (
              <div className="flex flex-col gap-2">
                <Skeleton className="h-8 w-full" />
                <Skeleton className="h-8 w-full" />
              </div>
            )}

            {activityQuery.isError && (
              <p className="text-sm text-destructive">Impossible de charger l&apos;activité.</p>
            )}

            {!activityQuery.isLoading && !activityQuery.isError && activityQuery.data?.length === 0 && (
              <p className="text-sm text-muted-foreground">Pas encore d&apos;activité.</p>
            )}

            {activityQuery.data?.map((event) => {
              const isWin = event.message.startsWith("a gagné");
              const isPick = event.message.startsWith("a misé");
              const Icon = isWin ? Trophy : isPick ? Ticket : UserPlus;
              return (
                <div key={`${event.targetType}-${event.targetId}-${event.occurredAt}`} className="flex items-start gap-2.5 text-sm">
                  <span className="relative mt-0.5 shrink-0">
                    <PlayerAvatar
                      number={event.avatarNumber}
                      position={event.avatarPosition}
                      colorway={event.avatarColorway}
                      icon={event.avatarIcon}
                      size="xs"
                    />
                    <span
                      className={`absolute -bottom-1 -right-1 flex h-4 w-4 items-center justify-center rounded-full bg-background ${isWin ? "text-success" : "text-muted-foreground"}`}
                    >
                      <Icon className="h-2.5 w-2.5" />
                    </span>
                  </span>
                  <div className="min-w-0 flex-1">
                    <Link
                      href={`/u/${encodeURIComponent(event.username)}`}
                      className="font-medium hover:underline"
                    >
                      {event.username}
                    </Link>{" "}
                    <span className="text-muted-foreground">{event.message}</span>
                    <p className="font-mono text-[11px] text-muted-foreground">
                      {formatRelativeTime(event.occurredAt)}
                    </p>
                    <div className="mt-1 flex flex-wrap gap-1">
                      {REACTION_EMOJIS.map((emoji) => {
                        const count = event.reactionCounts[emoji] ?? 0;
                        const mine = event.myReactions.includes(emoji);
                        return (
                          <button
                            key={emoji}
                            type="button"
                            disabled={reactMutation.isPending}
                            onClick={() =>
                              reactMutation.mutate({ targetType: event.targetType, targetId: event.targetId, emoji })
                            }
                            className={`flex items-center gap-1 rounded-full px-2 py-0.5 text-xs transition-colors ${
                              mine ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
                            }`}
                          >
                            <span>{emoji}</span>
                            {count > 0 && <span className="font-mono text-[10px]">{count}</span>}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                </div>
              );
            })}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
