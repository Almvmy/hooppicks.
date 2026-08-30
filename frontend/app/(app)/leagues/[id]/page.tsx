"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ArrowLeft, LogOut, Shield, Ticket, Trophy, UserPlus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { LeaderboardTable } from "@/components/leaderboard-table";
import { PlayerAvatar } from "@/components/player-avatar";
import {
  fetchLeagueActivity,
  fetchLeagueLeaderboard,
  fetchLeagueMembers,
  fetchMyLeagues,
  leaveLeague,
  reactToActivity,
} from "@/lib/api/leagues";
import { fetchProfile } from "@/lib/api/auth";
import { formatRelativeTime } from "@/lib/utils";

// Doit matcher ALLOWED_EMOJIS côté backend (LeagueService) : pas de sélecteur
// libre, un petit vocabulaire partagé suffit pour ce genre de réaction.
const REACTION_EMOJIS = ["👍", "🔥", "👎"];

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

  const leaderboardQuery = useQuery({
    queryKey: ["league-leaderboard", id],
    queryFn: () => fetchLeagueLeaderboard(id),
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
      <div className="flex items-start justify-between gap-4">
        <div>
          <Link
            href="/leagues"
            className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="h-4 w-4" />
            Mes ligues
          </Link>
          <div className="mt-2 flex items-center gap-2">
            <Shield className="h-6 w-6 text-primary" />
            <h1 className="font-heading text-2xl font-bold">{league?.name ?? "Ligue"}</h1>
          </div>
          {league && (
            <p className="mt-1 text-muted-foreground">
              {league.memberCount} membre{league.memberCount > 1 ? "s" : ""} · code{" "}
              <span className="font-mono font-bold text-foreground">{league.inviteCode}</span>
            </p>
          )}
        </div>

        <Button
          variant={confirmingLeave ? "destructive" : "outline"}
          size="sm"
          className="shrink-0 gap-1.5"
          onClick={() => (confirmingLeave ? leaveMutation.mutate() : setConfirmingLeave(true))}
          disabled={leaveMutation.isPending}
        >
          <LogOut className="h-3.5 w-3.5" />
          {confirmingLeave ? "Confirmer ?" : "Quitter"}
        </Button>
      </div>

      <div className="grid gap-4 lg:grid-cols-[1fr_260px]">
        <div className="glass relative overflow-hidden rounded-2xl">
          <LeaderboardTable
            entries={leaderboardQuery.data}
            isLoading={leaderboardQuery.isLoading}
            isError={leaderboardQuery.isError}
            emptyMessage="Aucun pari résolu dans cette ligue pour l'instant."
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
                  <span className="shrink-0 rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold uppercase text-primary">
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
                <div key={`${event.targetType}-${event.targetId}-${event.occurredAt}`} className="flex items-start gap-2 text-sm">
                  <Icon
                    className={`mt-0.5 h-3.5 w-3.5 shrink-0 ${isWin ? "text-primary" : "text-muted-foreground"}`}
                  />
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
