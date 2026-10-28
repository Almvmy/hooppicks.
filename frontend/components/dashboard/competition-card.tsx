"use client";

import Link from "next/link";
import { useQueries, useQuery } from "@tanstack/react-query";
import { CalendarDays, ChevronRight, Shield, Swords, Trophy } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { LeagueCrest } from "@/components/league-crest";
import { fetchLeaderboard, leaderboardQueryKey, type MyStanding } from "@/lib/api/leaderboard";
import { fetchLeagueLeaderboard, fetchMyLeagues } from "@/lib/api/leagues";
import { fetchDuels } from "@/lib/api/duels";
import type { LeaderboardEntry } from "@/lib/types";
import { cn, formatSignedPoints } from "@/lib/utils";

/** Écart au joueur devant : « Tu mènes », « 120 pts du 1er ». */
function gapLabel(entries: LeaderboardEntry[] | undefined, username: string | undefined): string | null {
  if (!entries || !username) return null;
  const me = entries.find((e) => e.username === username);
  if (!me) return null;
  if (me.rank === 1) return "Tu mènes";
  const leader = entries[0];
  return `${(leader.points - me.points).toLocaleString("fr-FR")} pts du 1er`;
}

function Row({
  href,
  icon: Icon,
  label,
  value,
  hint,
  accent = false,
}: {
  href: string;
  icon: React.ElementType;
  label: string;
  value: React.ReactNode;
  hint?: string | null;
  accent?: boolean;
}) {
  return (
    <Link href={href} className="group flex items-center gap-3 py-2.5">
      <span className={cn("flex h-8 w-8 shrink-0 items-center justify-center rounded-xl", accent ? "glass-accent" : "glass-inset")}>
        <Icon className={cn("h-4 w-4", accent ? "text-primary" : "text-muted-foreground")} />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block text-sm font-medium group-hover:underline">{label}</span>
        {hint && <span className="block truncate text-xs text-muted-foreground">{hint}</span>}
      </span>
      <span className="shrink-0 font-mono text-sm font-bold">{value}</span>
      <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" />
    </Link>
  );
}

/**
 * Toute la compétition en une carte : saison, course au champion du mois,
 * duel de la semaine et place dans chaque ligue. Remplace l'aperçu du
 * classement (le top 5, déjà sur la page Classement) et celui des ligues.
 */
export function CompetitionCard({ standing, username }: { standing: MyStanding | undefined; username: string | undefined }) {
  const month = useQuery({ queryKey: leaderboardQueryKey("month"), queryFn: () => fetchLeaderboard("month"), staleTime: 2 * 60_000 });
  const duels = useQuery({ queryKey: ["duels"], queryFn: fetchDuels, staleTime: 60_000 });
  const leagues = useQuery({ queryKey: ["leagues"], queryFn: fetchMyLeagues, staleTime: 5 * 60_000 });
  // Même clé que la page Ligues : l'ouvrir ensuite est instantané.
  const leagueBoards = useQueries({
    queries: (leagues.data ?? []).map((l) => ({
      queryKey: ["league-leaderboard", l.id],
      queryFn: () => fetchLeagueLeaderboard(l.id),
      staleTime: 60_000,
    })),
  });

  const monthMe = month.data?.find((e) => e.username === username);
  const live = duels.data?.filter((d) => d.status === "accepted") ?? [];
  const incoming = duels.data?.filter((d) => d.status === "pending" && !d.iChallenged).length ?? 0;

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle className="font-heading text-base">Compétition</CardTitle>
        <Link href="/leaderboard" className="text-xs font-medium text-primary hover:underline">
          Classement
        </Link>
      </CardHeader>
      <CardContent className="flex flex-col divide-y divide-tint/10">
        {!standing ? (
          <Skeleton className="my-2 h-10 w-full" />
        ) : (
          <Row
            href="/leaderboard"
            icon={Trophy}
            label="Saison"
            value={standing.seasonRank != null ? `#${standing.seasonRank}` : "-"}
            hint={
              standing.seasonRank != null
                ? `${formatSignedPoints(standing.seasonPoints)} · ${standing.seasonPlayers} joueurs`
                : "Pas encore classé"
            }
          />
        )}

        <Row
          href="/leaderboard"
          icon={CalendarDays}
          label="Champion du mois"
          accent={monthMe?.rank === 1}
          value={monthMe ? `#${monthMe.rank}` : "-"}
          hint={monthMe ? gapLabel(month.data, username) : "Un ticket réglé ce mois-ci pour entrer dans la course"}
        />

        {live.length > 0 ? (
          live.slice(0, 2).map((d) => {
            const mine = d.myPoints ?? 0;
            const theirs = d.opponentPoints ?? 0;
            return (
              <Row
                key={d.id}
                href="/duels"
                icon={Swords}
                label={`Duel contre @${d.opponentUsername}`}
                accent={mine > theirs}
                value={`${mine} – ${theirs}`}
                hint={mine === theirs ? "À égalité" : mine > theirs ? "Tu mènes" : "Tu es derrière"}
              />
            );
          })
        ) : (
          <Row
            href="/duels"
            icon={Swords}
            label={incoming > 0 ? `${incoming} défi${incoming > 1 ? "s" : ""} reçu${incoming > 1 ? "s" : ""}` : "Duel de la semaine"}
            accent={incoming > 0}
            value=""
            hint={incoming > 0 ? "Accepte ou décline" : "Défie un pote sur la semaine"}
          />
        )}

        {(leagues.data ?? []).map((l, i) => {
          const board = leagueBoards[i]?.data;
          const me = board?.find((e) => e.username === username);
          return (
            <Link key={l.id} href={`/leagues/${l.id}`} className="group flex items-center gap-3 py-2.5">
              <LeagueCrest name={l.name} size={32} />
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-medium group-hover:underline">{l.name}</span>
                <span className="block truncate text-xs text-muted-foreground">{gapLabel(board, username) ?? `${l.memberCount} membres`}</span>
              </span>
              <span className="shrink-0 font-mono text-sm font-bold">
                {me ? `#${me.rank}` : "-"}
                <span className="font-normal text-muted-foreground">/{l.memberCount}</span>
              </span>
              <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" />
            </Link>
          );
        })}

        {leagues.data?.length === 0 && (
          <Row href="/leagues" icon={Shield} label="Ligues" value="" hint="Crée une ligue avec tes potes" />
        )}
      </CardContent>
    </Card>
  );
}
