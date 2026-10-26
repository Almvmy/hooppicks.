"use client";

import { use, useEffect, useState } from "react";
import type { LeaderboardPeriod, PhaseChampions, WeekChampions } from "@/lib/types";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Activity, ArrowLeft, Copy, LogOut, Share2, Ticket, TrendingUp, Trophy, UserPlus, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { LeaderboardTable } from "@/components/leaderboard-table";
import { PlayerAvatar } from "@/components/player-avatar";
import { LeagueCrest, hueFromName } from "@/components/league-crest";
import { ChampionBanner } from "@/components/leaderboard/champion-banner";
import { Podium } from "@/components/leaderboard/podium";
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
import { fetchPhaseChampions, fetchWeekChampions } from "@/lib/api/palmares";
import { championPhase, periodOption, useLeaderboardPeriods } from "@/lib/leaderboard-periods";
import { formatRelativeTime, formatSignedPoints, cn } from "@/lib/utils";

// Doit matcher ALLOWED_EMOJIS côté backend (LeagueService) : pas de sélecteur
// libre, un petit vocabulaire partagé suffit pour ce genre de réaction.
const REACTION_EMOJIS = ["👍", "🔥", "👎"];

type Tab = "classement" | "membres" | "activite";

export default function LeagueDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();
  const queryClient = useQueryClient();
  const [confirmingLeave, setConfirmingLeave] = useState(false);
  const [tab, setTab] = useState<Tab>("classement");

  const leaguesQuery = useQuery({ queryKey: ["leagues"], queryFn: fetchMyLeagues });
  const league = leaguesQuery.data?.find((l) => l.id === id);

  // La semaine relance la course entre amis chaque lundi, quand l'écart
  // de la saison est devenu trop grand pour être comblé.
  const [period, setPeriod] = useState<LeaderboardPeriod>("week");
  const periods = useLeaderboardPeriods();
  // Champion(s) de la ligue : la semaine passée, le mois passé sur « Ce
  // mois-ci », la phase sur la Coupe NBA, les playoffs et le général.
  const phase = championPhase(period);
  const championsQuery = useQuery<WeekChampions | PhaseChampions>({
    queryKey: phase ? ["phase-champions", phase, id] : ["week-champions", id],
    queryFn: () => (phase ? fetchPhaseChampions(phase, id) : fetchWeekChampions(id)),
    staleTime: 10 * 60 * 1000,
  });
  const champions = new Set(championsQuery.data?.champions.map((c) => c.username));
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
  const ranks = new Map(leaderboard.map((e) => [e.username, e]));
  const hue = hueFromName(league?.name ?? "Ligue");

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

  const tabs: { value: Tab; label: string; icon: React.ElementType }[] = [
    { value: "classement", label: "Classement", icon: Trophy },
    { value: "membres", label: `Membres${membersQuery.data ? ` · ${membersQuery.data.length}` : ""}`, icon: Users },
    { value: "activite", label: "Activité", icon: Activity },
  ];

  return (
    <div className="flex flex-col gap-5">
      <Link
        href="/leagues"
        className="flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" />
        Mes ligues
      </Link>

      {/* En-tête aux couleurs du blason : chaque ligue a sa teinte. */}
      <div
        className="glass relative overflow-hidden rounded-3xl p-5 sm:p-6"
        style={{ background: `linear-gradient(135deg, hsl(${hue} 55% 30% / 0.35), transparent 70%)` }}
      >
        <div className="flex items-center gap-4">
          <LeagueCrest name={league?.name ?? "Ligue"} size={72} />
          <div className="min-w-0 flex-1">
            <h1 className="line-clamp-2 break-words font-heading text-2xl font-bold leading-tight sm:text-3xl">{league?.name ?? "Ligue"}</h1>
            {league && (
              <p className="mt-1 text-sm text-muted-foreground">
                {league.memberCount} membre{league.memberCount > 1 ? "s" : ""}
                {leaderboard[0] && (
                  <>
                    {" · "}
                    <span className="text-foreground">{leaderboard[0].username}</span> en tête
                  </>
                )}
              </p>
            )}
          </div>
        </div>
        {league && (
          <div className="mt-4 flex items-center gap-2">
            <Button size="sm" onClick={() => shareLeagueInvite(league)}>
              <Share2 className="h-3.5 w-3.5" />
              Inviter
            </Button>
            <button
              type="button"
              onClick={() => copyLeagueCode(league.inviteCode)}
              className="glass-inset flex h-8 items-center gap-1.5 rounded-xl px-2.5 font-mono text-xs font-bold tracking-widest text-muted-foreground transition-colors hover:text-foreground"
              aria-label={`Copier le code ${league.inviteCode}`}
            >
              {league.inviteCode}
              <Copy className="h-3 w-3" />
            </button>
          </div>
        )}
      </div>

      <div className="glass-inset-quiet flex gap-1 rounded-2xl p-1" role="tablist" aria-label="Rubrique de la ligue">
        {tabs.map((t) => (
          <button
            key={t.value}
            type="button"
            role="tab"
            aria-selected={tab === t.value}
            onClick={() => setTab(t.value)}
            className={cn(
              "flex min-h-10 flex-1 items-center justify-center gap-1.5 rounded-xl px-2 text-sm font-semibold transition-colors",
              tab === t.value ? "glass-accent" : "text-muted-foreground hover:text-foreground"
            )}
          >
            <t.icon className="h-4 w-4 shrink-0" />
            <span className="truncate">{t.label}</span>
          </button>
        ))}
      </div>

      {tab === "classement" && (
        <div className="flex flex-col gap-4" role="tabpanel">
          <div className="glass-scroll flex gap-2 overflow-x-auto" role="group" aria-label="Période du classement">
            {periods.map((p) => (
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

          {championsQuery.data && <ChampionBanner data={championsQuery.data} shareable={false} />}

          {me && (
            <div className="grid grid-cols-3 gap-2">
              <div className="glass-accent rounded-2xl px-3 py-2.5">
                <p className="text-[11px] uppercase tracking-wide text-muted-foreground">Ta place</p>
                <p className="font-heading text-2xl font-bold">
                  #{me.rank}
                  <span className="text-sm font-normal text-muted-foreground"> / {leaderboard.length}</span>
                </p>
              </div>
              <div className="glass rounded-2xl px-3 py-2.5">
                <p className="text-[11px] uppercase tracking-wide text-muted-foreground">Tes points</p>
                <p className="font-mono text-xl font-bold">{formatSignedPoints(me.points, false)}</p>
              </div>
              <div className="glass rounded-2xl px-3 py-2.5">
                <p className="flex items-center gap-1 text-[11px] uppercase tracking-wide text-muted-foreground">
                  <TrendingUp className="h-3 w-3" />
                  Objectif
                </p>
                <p className="text-xs font-medium leading-snug">
                  {me.rank === 1
                    ? "Tu mènes la ligue."
                    : above
                      ? `${(above.points - me.points).toLocaleString("fr-FR")} pts pour doubler ${above.username}`
                      : "À égalité devant."}
                </p>
              </div>
            </div>
          )}

          {leaderboard.length > 0 && (
            <Podium entries={leaderboard} username={profile?.username} champions={champions} />
          )}

          {(leaderboardQuery.isLoading || leaderboardQuery.isError || leaderboard.length !== 3) && (
            <div className="glass relative overflow-hidden rounded-2xl">
              <LeaderboardTable
                entries={leaderboardQuery.data?.slice(3)}
                isLoading={leaderboardQuery.isLoading}
                isError={leaderboardQuery.isError}
                emptyMessage={
                  leaderboard.length > 0
                    ? "Tout le monde est sur le podium."
                    : period === "week"
                      ? "Aucun ticket réglé dans la ligue cette semaine."
                      : period === "month"
                        ? "Aucun ticket réglé dans la ligue ce mois-ci."
                        : phase
                          ? `Aucun ticket ${periodOption(period).scope} réglé dans la ligue.`
                          : "Aucun ticket réglé dans cette ligue pour l'instant."
                }
                currentUsername={profile?.username}
                champions={champions}
              />
            </div>
          )}
        </div>
      )}

      {tab === "membres" && (
        <Card role="tabpanel">
          <CardContent className="flex flex-col divide-y divide-tint/10 pt-2">
            {membersQuery.isLoading && (
              <div className="flex flex-col gap-2 py-3">
                <Skeleton className="h-9 w-full" />
                <Skeleton className="h-9 w-full" />
                <Skeleton className="h-9 w-full" />
              </div>
            )}

            {membersQuery.isError && (
              <p className="py-4 text-sm text-destructive">Impossible de charger les membres.</p>
            )}

            {[...(membersQuery.data ?? [])]
              .sort((a, b) => (ranks.get(a.username)?.rank ?? Infinity) - (ranks.get(b.username)?.rank ?? Infinity))
              .map((member) => {
              const entry = ranks.get(member.username);
              return (
                <Link
                  key={member.username}
                  href={`/u/${encodeURIComponent(member.username)}`}
                  className="group flex items-center gap-3 py-3"
                >
                  <PlayerAvatar
                    number={member.avatarNumber}
                    position={member.avatarPosition}
                    colorway={member.avatarColorway}
                    icon={member.avatarIcon}
                    size="sm"
                  />
                  <div className="min-w-0 flex-1">
                    <p className="flex items-center gap-2 truncate font-medium group-hover:underline">
                      {member.username}
                      {member.username === profile?.username && (
                        <span className="text-[11px] font-semibold text-primary">toi</span>
                      )}
                    </p>
                    {member.isOwner && (
                      <span className="text-[10px] font-semibold uppercase tracking-wide text-paint">Créateur</span>
                    )}
                  </div>
                  <div className="text-right">
                    <p className="font-mono text-sm font-bold">{entry ? formatSignedPoints(entry.points) : "-"}</p>
                    <p className="text-[11px] text-muted-foreground">
                      {entry ? `#${entry.rank} · ${periodOption(period).label.toLowerCase()}` : "pas classé"}
                    </p>
                  </div>
                </Link>
              );
            })}
          </CardContent>
        </Card>
      )}

      {tab === "activite" && (
        <Card role="tabpanel">
          <CardContent className="flex flex-col gap-4 pt-6">
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
                      size="sm"
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
      )}

      {/* En bas de page, loin du bouton « Inviter » : une action qu'on ne veut pas faire par erreur. */}
      <Button
        variant={confirmingLeave ? "destructive" : "ghost"}
        size="sm"
        className="mt-2 w-fit gap-1.5 self-center text-muted-foreground"
        onClick={() => (confirmingLeave ? leaveMutation.mutate() : setConfirmingLeave(true))}
        disabled={leaveMutation.isPending}
      >
        <LogOut className="h-3.5 w-3.5" />
        {confirmingLeave ? "Confirmer : quitter la ligue" : "Quitter la ligue"}
      </Button>
    </div>
  );
}
