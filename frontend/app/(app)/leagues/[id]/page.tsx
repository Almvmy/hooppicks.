"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ArrowLeft, LogOut, Shield, Trophy, UserPlus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { LeaderboardTable } from "@/components/leaderboard-table";
import {
  fetchLeagueActivity,
  fetchLeagueLeaderboard,
  fetchLeagueMembers,
  fetchMyLeagues,
  leaveLeague,
} from "@/lib/api/leagues";
import { formatRelativeTime } from "@/lib/utils";

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
              {league.memberCount} membre{league.memberCount > 1 ? "s" : ""} — code{" "}
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
        <div className="relative overflow-hidden rounded-lg border border-border bg-card">
          <LeaderboardTable
            entries={leaderboardQuery.data}
            isLoading={leaderboardQuery.isLoading}
            isError={leaderboardQuery.isError}
            emptyMessage="Aucun pari résolu dans cette ligue pour l'instant."
          />
        </div>

        <Card className="border-border bg-card">
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

            {membersQuery.data?.map((member) => (
              <div key={member.username} className="flex items-center justify-between text-sm">
                <span className="truncate font-medium">{member.username}</span>
                {member.isOwner && (
                  <span className="shrink-0 rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold uppercase text-primary">
                    Créateur
                  </span>
                )}
              </div>
            ))}
          </CardContent>
        </Card>

        <Card className="border-border bg-card">
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

            {!activityQuery.isLoading && activityQuery.data?.length === 0 && (
              <p className="text-sm text-muted-foreground">Pas encore d&apos;activité.</p>
            )}

            {activityQuery.data?.map((event, i) => {
              const isWin = event.message.startsWith("a gagné");
              const Icon = isWin ? Trophy : UserPlus;
              return (
                <div key={i} className="flex items-start gap-2 text-sm">
                  <Icon
                    className={`mt-0.5 h-3.5 w-3.5 shrink-0 ${isWin ? "text-primary" : "text-muted-foreground"}`}
                  />
                  <div>
                    <span className="font-medium">{event.username}</span>{" "}
                    <span className="text-muted-foreground">{event.message}</span>
                    <p className="font-mono text-[11px] text-muted-foreground">
                      {formatRelativeTime(event.occurredAt)}
                    </p>
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
