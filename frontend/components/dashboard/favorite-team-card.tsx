"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Activity, CalendarDays, ChevronRight, Heart, Star, Users } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { TeamLogo } from "@/components/team-logo";
import { OddsButton } from "@/components/odds-button";
import { FavoriteTeamPicker } from "@/components/favorite-team-picker";
import { PlayerCardDialog } from "@/components/player-card-dialog";
import { InjuryPill, PlayerHeadshot, playerName } from "@/components/players/player-tile";
import { TeamNews } from "@/components/news/team-news";
import { fetchTeamRoster } from "@/lib/api/teams";
import { fetchFavoriteTeamStats } from "@/lib/api/favorite-team";
import { rankFanTeams } from "@/lib/fan-teams";
import { getTeamColor } from "@/lib/team-colors";
import { useFavoriteTeam, useTeamsByAbbreviation } from "@/lib/use-teams";
import { FavoriteTeamStats, LeaderboardEntry, Match, RosterPlayer, TeamRank } from "@/lib/types";
import { bettingClosedReason, cn, formatMatchTime, getDayLabel, isBettable, winChances } from "@/lib/utils";

function zoneOf(seed: number | null): { label: string; className: string } | null {
  if (seed === null) return null;
  if (seed <= 6) return { label: "Playoffs", className: "bg-success/15 text-success" };
  if (seed <= 10) return { label: "Play-in", className: "bg-primary/15 text-primary" };
  return { label: "Hors course", className: "bg-tint/10 text-muted-foreground" };
}

// Série ESPN ("W3" / "L2") en clair.
function streakLabel(streak: string | null): string | null {
  const m = streak?.match(/^([WL])(\d+)$/);
  if (!m) return null;
  const n = Number(m[2]);
  if (m[1] === "W") return `${n} victoire${n > 1 ? "s" : ""} de suite`;
  return `${n} défaite${n > 1 ? "s" : ""} de suite`;
}

function Block({ title, icon: Icon, children, className }: { title: string; icon: typeof Star; children: React.ReactNode; className?: string }) {
  return (
    <div className={cn("glass-inset-quiet flex flex-col gap-2 rounded-2xl p-3.5", className)}>
      <p className="flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
        <Icon className="h-3.5 w-3.5" />
        {title}
      </p>
      {children}
    </div>
  );
}

function NextMatch({ match, team }: { match: Match | undefined; team: TeamRank }) {
  if (!match) return <p className="text-sm text-muted-foreground">Pas de match programmé pour l&apos;instant.</p>;
  const isHome = match.homeTeam.abbreviation === team.abbreviation;
  const opponent = isHome ? match.awayTeam : match.homeTeam;
  const date = new Date(match.date);
  const label = `${match.awayTeam.name} vs ${match.homeTeam.name}`;
  // Ton équipe d'abord, l'adversaire ensuite.
  const sides = (isHome ? ["home", "away"] : ["away", "home"]) as ("home" | "away")[];

  return (
    <div className="flex flex-col gap-2">
      <Link href={`/matches/${match.id}`} className="flex items-center gap-2 text-sm hover:underline">
        <span className="text-muted-foreground">{isHome ? "vs" : "@"}</span>
        <TeamLogo abbreviation={opponent.abbreviation} logoUrl={opponent.logoUrl} size={22} />
        <span className="font-medium">{opponent.name}</span>
        <span className="ml-auto font-mono text-xs text-muted-foreground">
          {getDayLabel(date)} · {formatMatchTime(date)}
        </span>
      </Link>
      {isBettable(match) ? (
        <div className="flex gap-2">
          {sides.map((side) => {
            const t = side === "home" ? match.homeTeam : match.awayTeam;
            const odds = side === "home" ? match.odds.moneylineHome : match.odds.moneylineAway;
            return (
              <OddsButton
                key={side}
                impliedProbability={winChances(match.odds.moneylineHome, match.odds.moneylineAway)[side === "home" ? 0 : 1]}
                selection={{
                  id: `${match.id}-moneyline-${side}`,
                  matchId: match.id,
                  matchLabel: label,
                  market: "moneyline",
                  outcome: side,
                  label: `${t.abbreviation} (V)`,
                  odds,
                }}
              />
            );
          })}
        </div>
      ) : (
        <p className={cn("text-xs first-letter:uppercase", match.status === "live" ? "font-semibold text-live" : "text-muted-foreground")}>
          {bettingClosedReason(match)}
        </p>
      )}
    </div>
  );
}

function LastResult({ match, team }: { match: Match | undefined; team: TeamRank }) {
  if (!match || match.homeScore == null || match.awayScore == null) return null;
  const isHome = match.homeTeam.abbreviation === team.abbreviation;
  const scored = isHome ? match.homeScore : match.awayScore;
  const conceded = isHome ? match.awayScore : match.homeScore;
  const opponent = isHome ? match.awayTeam : match.homeTeam;
  const won = scored > conceded;
  return (
    <Link href={`/matches/${match.id}`} className="flex items-center gap-2 text-xs text-muted-foreground hover:underline">
      <span className={cn("rounded-full px-1.5 py-px text-[10px] font-bold", won ? "bg-success/15 text-success" : "bg-destructive/15 text-destructive")}>
        {won ? "V" : "D"}
      </span>
      Dernier match : {scored}-{conceded} {isHome ? "contre" : "chez"} {opponent.abbreviation}
    </Link>
  );
}

function Community({ entries, team, username }: { entries: LeaderboardEntry[]; team: TeamRank; username: string | undefined }) {
  const teams = useTeamsByAbbreviation();
  const ranking = useMemo(() => rankFanTeams(entries, teams), [entries, teams]);
  const index = ranking.findIndex((t) => t.abbreviation === team.abbreviation);
  if (index < 0) {
    return <p className="text-sm text-muted-foreground">Aucun supporter classé pour l&apos;instant : un ticket résolu et tu ouvres la marche.</p>;
  }
  const community = ranking[index];
  const myIndex = community.fans.findIndex((f) => f.username === username);
  return (
    <div className="flex flex-col gap-1 text-sm">
      <p>
        <span className="font-heading text-xl font-bold">#{index + 1}</span>
        <span className="text-muted-foreground"> sur {ranking.length} communauté{ranking.length > 1 ? "s" : ""}</span>
      </p>
      <p className="text-xs text-muted-foreground">
        {community.fans.length} supporter{community.fans.length > 1 ? "s" : ""} · {community.averagePoints.toLocaleString("fr-FR")} pts / fan
      </p>
      <p className="text-xs">
        {myIndex >= 0 ? (
          <>
            Tu es <strong>{myIndex + 1}<sup>{myIndex === 0 ? "er" : "e"}</sup></strong> de ses fans
            {myIndex === 0 && " : le meilleur supporter !"}
          </>
        ) : (
          <span className="text-muted-foreground">Tu n&apos;es pas encore classé parmi ses fans.</span>
        )}
      </p>
      <Link href="/leaderboard?vue=fans" className="mt-1 text-xs font-medium text-primary hover:underline">
        Classement des fans
      </Link>
    </div>
  );
}

function rate(won: number, lost: number): number | null {
  return won + lost === 0 ? null : Math.round((won * 100) / (won + lost));
}

function BetsOnTeam({ stats }: { stats: FavoriteTeamStats | undefined }) {
  if (!stats) return <Skeleton className="h-16 w-full rounded-xl" />;
  if (stats.forLegs + stats.againstLegs === 0) {
    return <p className="text-sm text-muted-foreground">Aucun pari sur ses matchs pour l&apos;instant.</p>;
  }
  const forRate = rate(stats.forWon, stats.forLost);
  const againstRate = rate(stats.againstWon, stats.againstLost);
  // Verdict seulement quand les deux côtés ont des résultats : sinon on
  // comparerait un taux à rien.
  const verdict =
    forRate !== null && againstRate !== null && forRate !== againstRate
      ? forRate > againstRate
        ? "Ton cœur a raison : tu réussis mieux en pariant pour elle."
        : "Tu es plus lucide en pariant contre elle."
      : null;
  return (
    <div className="flex flex-col gap-2 text-sm">
      <div className="grid grid-cols-2 gap-2">
        {[
          { label: "Pour", legs: stats.forLegs, rate: forRate },
          { label: "Contre", legs: stats.againstLegs, rate: againstRate },
        ].map((side) => (
          <div key={side.label} className="flex flex-col">
            <span className="text-xs text-muted-foreground">
              {side.label} · {side.legs} sélection{side.legs > 1 ? "s" : ""}
            </span>
            <span className="font-mono text-lg font-bold">{side.rate === null ? "-" : `${side.rate}%`}</span>
          </div>
        ))}
      </div>
      {verdict && <p className="text-xs text-muted-foreground">{verdict}</p>}
    </div>
  );
}

/**
 * Carte « Mon équipe » du tableau de bord : situation, prochain match
 * (pariable directement), infirmerie, joueurs clés, communauté de fans,
 * tes paris sur elle et ses actus. Sans équipe favorite : invite à en choisir une.
 */
export function FavoriteTeamCard({
  matches,
  leaderboard,
  username,
  favoriteTeamName,
}: {
  matches: Match[] | undefined;
  leaderboard: LeaderboardEntry[];
  username: string | undefined;
  favoriteTeamName: string | undefined;
}) {
  const { team, isLoading } = useFavoriteTeam();
  const [selectedPlayer, setSelectedPlayer] = useState<RosterPlayer | null>(null);

  const rosterQuery = useQuery({
    queryKey: ["team-roster", team?.id],
    queryFn: () => fetchTeamRoster(team!.id),
    enabled: !!team,
    staleTime: 60 * 60 * 1000,
  });
  const statsQuery = useQuery({
    queryKey: ["favorite-team-stats", team?.abbreviation],
    queryFn: fetchFavoriteTeamStats,
    enabled: !!team,
    staleTime: 5 * 60 * 1000,
  });

  const { next, last } = useMemo(() => {
    const own = (matches ?? []).filter(
      (m) => team && (m.homeTeam.abbreviation === team.abbreviation || m.awayTeam.abbreviation === team.abbreviation)
    );
    const byDate = (a: Match, b: Match) => new Date(a.date).getTime() - new Date(b.date).getTime();
    return {
      next: own.filter((m) => m.status !== "finished").sort(byDate)[0],
      last: own.filter((m) => m.status === "finished").sort(byDate).at(-1),
    };
  }, [matches, team]);

  if (isLoading) return <Skeleton className="h-72 w-full rounded-3xl" />;

  if (!team) {
    return (
      <Card>
        <CardContent className="flex flex-col gap-3 pt-6">
          <p className="flex items-center gap-2 font-heading text-base font-bold">
            <Heart className="h-4 w-4 text-primary" />
            Choisis ton équipe favorite
          </p>
          <p className="text-sm text-muted-foreground">
            Son prochain match à parier, ses blessés, ses actus, ta place parmi ses supporters : tout arrive ici.
          </p>
          <FavoriteTeamPicker currentTeam={favoriteTeamName ?? ""} />
        </CardContent>
      </Card>
    );
  }

  const color = getTeamColor(team.abbreviation);
  const zone = zoneOf(team.conferenceSeed);
  const streak = streakLabel(team.streak);
  const roster = rosterQuery.data ?? [];
  const injured = roster.filter((p) => p.injuryStatus);
  const keyPlayers = [...roster]
    .filter((p) => p.pointsPerGame !== null)
    .sort((a, b) => (b.pointsPerGame ?? 0) - (a.pointsPerGame ?? 0))
    .slice(0, 3);

  return (
    <section
      className="glass relative overflow-hidden rounded-3xl p-5 sm:p-6"
      style={{
        backgroundImage: `linear-gradient(115deg, color-mix(in srgb, ${color} 22%, transparent) 0%, transparent 60%), var(--glass-tint)`,
      }}
      aria-label={`Mon équipe : ${team.name}`}
    >
      <div aria-hidden className="pointer-events-none absolute -right-12 -top-10 rotate-12 opacity-[0.10]">
        <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={220} />
      </div>

      <div className="relative flex flex-col gap-4">
        <div className="flex flex-wrap items-center gap-3">
          <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={52} />
          <div className="min-w-0 flex-1">
            <p className="flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
              <Heart className="h-3 w-3 fill-current text-primary" />
              Mon équipe
            </p>
            <h2 className="truncate font-heading text-xl font-bold">{team.name}</h2>
            <p className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-muted-foreground">
              {team.wins !== null && team.losses !== null && (
                <span className="font-mono font-semibold text-foreground">
                  {team.wins}V - {team.losses}D
                </span>
              )}
              {team.conferenceSeed !== null && (
                <span>
                  {team.conferenceSeed}
                  <sup>{team.conferenceSeed === 1 ? "re" : "e"}</sup> à l&apos;{team.conference}
                </span>
              )}
              {zone && <span className={cn("rounded-full px-2 py-px text-[10px] font-bold uppercase", zone.className)}>{zone.label}</span>}
              {streak && <span className="text-xs">· {streak}</span>}
            </p>
          </div>
          <Link
            href={`/teams/${team.id}`}
            className="flex items-center gap-0.5 text-sm font-medium text-primary hover:underline"
          >
            Page de l&apos;équipe
            <ChevronRight className="h-4 w-4" />
          </Link>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <Block title="Prochain match" icon={CalendarDays} className="sm:col-span-2">
            <NextMatch match={next} team={team} />
            <LastResult match={last} team={team} />
          </Block>

          <Block title="Infirmerie" icon={Activity}>
            {rosterQuery.isLoading && <Skeleton className="h-10 w-full rounded-xl" />}
            {rosterQuery.isError && <p className="text-sm text-muted-foreground">Effectif indisponible.</p>}
            {!rosterQuery.isLoading && !rosterQuery.isError && injured.length === 0 && (
              <p className="text-sm text-muted-foreground">Aucun blessé signalé.</p>
            )}
            {injured.slice(0, 4).map((p) => (
              <button
                key={p.id}
                type="button"
                onClick={() => setSelectedPlayer(p)}
                className="flex items-center gap-2 text-left text-sm hover:underline"
              >
                <PlayerHeadshot player={p} size={24} />
                <span className="min-w-0 flex-1 truncate">{playerName(p)}</span>
                <InjuryPill status={p.injuryStatus} />
              </button>
            ))}
            {injured.length > 4 && <p className="text-xs text-muted-foreground">et {injured.length - 4} autre(s)</p>}
          </Block>

          <Block title="Joueurs clés" icon={Star}>
            {rosterQuery.isLoading && <Skeleton className="h-10 w-full rounded-xl" />}
            {!rosterQuery.isLoading && keyPlayers.length === 0 && !rosterQuery.isError && (
              <p className="text-sm text-muted-foreground">Stats de la saison pas encore disponibles.</p>
            )}
            {keyPlayers.map((p) => (
              <button
                key={p.id}
                type="button"
                onClick={() => setSelectedPlayer(p)}
                className="flex items-center gap-2 text-left text-sm hover:underline"
              >
                <PlayerHeadshot player={p} size={28} />
                <span className="min-w-0 flex-1 truncate">{playerName(p)}</span>
                <span className="shrink-0 font-mono text-xs text-muted-foreground">
                  {p.pointsPerGame?.toFixed(1)} pts · {p.reboundsPerGame?.toFixed(1)} reb · {p.assistsPerGame?.toFixed(1)} pd
                </span>
              </button>
            ))}
          </Block>

          <Block title="Ta communauté" icon={Users}>
            <Community entries={leaderboard} team={team} username={username} />
          </Block>

          <Block title="Tes paris sur elle" icon={Heart}>
            {statsQuery.isError ? (
              <p className="text-sm text-muted-foreground">Statistiques indisponibles.</p>
            ) : (
              <BetsOnTeam stats={statsQuery.data} />
            )}
          </Block>
        </div>

        <TeamNews abbreviations={[team.abbreviation]} limit={2} />
      </div>

      <PlayerCardDialog player={selectedPlayer} onOpenChange={(open) => !open && setSelectedPlayer(null)} />
    </section>
  );
}
