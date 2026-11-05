"use client";

import { useState } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, X } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { PlayerAvatar } from "@/components/player-avatar";
import { ShareButton } from "@/components/share-button";
import { TeamLogo } from "@/components/team-logo";
import { fetchBracket, pairOf, roundLabel, saveBracketPicks, withPick, type Bracket, type BracketGame } from "@/lib/api/bracket";
import { useTeamsByAbbreviation } from "@/lib/use-teams";
import { cn, formatShortDateTime } from "@/lib/utils";

export default function CoupePage() {
  const query = useQuery({ queryKey: ["bracket"], queryFn: fetchBracket });

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-4">
      <h1 className="font-heading text-2xl font-bold">{query.data?.label ?? "Coupe NBA"}</h1>
      {query.isLoading && <Skeleton className="h-64 w-full rounded-2xl" />}
      {query.isError && <p className="text-sm text-destructive">Impossible de charger le tableau.</p>}
      {query.data === null && (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-8 text-center">
            <p className="font-heading text-lg font-bold">Le tableau ouvre à la fin des poules</p>
            <Link href="/leaderboard" className="text-sm font-semibold text-primary hover:underline">
              Voir le classement
            </Link>
          </CardContent>
        </Card>
      )}
      {query.data && (query.data.locked ? <LockedBracket bracket={query.data} /> : <OpenBracket key={query.data.id} bracket={query.data} />)}
    </div>
  );
}

function useTeamLabel() {
  const teams = useTeamsByAbbreviation();
  return (abbreviation: string) => teams.get(abbreviation)?.name ?? abbreviation;
}

function roundsOf(games: BracketGame[]): { round: number; label: string; points: number; games: BracketGame[] }[] {
  const rounds = [...new Set(games.map((g) => g.round))];
  return rounds.map((round) => {
    const inRound = games.filter((g) => g.round === round);
    return { round, label: roundLabel(games, round), points: inRound[0].points, games: inRound };
  });
}

function TeamChoice({ team, chosen, disabled, onPick }: { team: string | null; chosen: boolean; disabled: boolean; onPick: () => void }) {
  const teams = useTeamsByAbbreviation();
  const info = team ? teams.get(team) : undefined;
  return (
    <button
      type="button"
      disabled={disabled || !team}
      aria-pressed={chosen}
      onClick={onPick}
      className={cn(
        "flex min-h-12 min-w-0 flex-1 items-center gap-2 rounded-xl px-2.5 py-2 text-left text-sm font-semibold transition-colors",
        chosen ? "glass-accent" : "glass-inset-quiet",
        team ? "hover:text-primary" : "justify-center text-muted-foreground"
      )}
    >
      {team ? (
        <>
          <TeamLogo abbreviation={team} logoUrl={info?.logoUrl} size={26} />
          <span className="min-w-0 truncate">{info?.name ?? team}</span>
        </>
      ) : (
        "À choisir"
      )}
    </button>
  );
}

function OpenBracket({ bracket }: { bracket: Bracket }) {
  const queryClient = useQueryClient();
  const teamLabel = useTeamLabel();
  const [draft, setDraft] = useState<(string | null)[] | null>(null);
  const picks = draft ?? bracket.myPicks ?? bracket.games.map(() => null);
  const complete = picks.every(Boolean);
  const dirty = draft !== null;
  const champion = picks[picks.length - 1];

  const save = useMutation({
    mutationFn: () => saveBracketPicks(picks as string[]),
    onSuccess: (fresh) => {
      queryClient.setQueryData(["bracket"], fresh);
      setDraft(null);
      toast.success("Tableau enregistré");
    },
    onError: (e: Error) => {
      toast.error(e.message || "Enregistrement impossible.");
      queryClient.invalidateQueries({ queryKey: ["bracket"] });
    },
  });

  return (
    <div className="flex flex-col gap-4">
      <p className="text-sm text-muted-foreground">
        Avant le {formatShortDateTime(new Date(bracket.locksAt))} · {bracket.players} tableau{bracket.players > 1 ? "x" : ""} rempli{bracket.players > 1 ? "s" : ""}
      </p>

      {roundsOf(bracket.games).map((round) => (
        <section key={round.round} className="flex flex-col gap-2">
          <h2 className="flex items-baseline justify-between gap-2 font-heading text-base font-bold">
            {round.label}
            <span className="font-mono text-xs font-normal text-muted-foreground">{round.points} pts</span>
          </h2>
          {round.games.map((game) => {
            const [a, b] = pairOf(bracket.games, bracket.teams, picks, game.index);
            return (
              <div key={game.index} className="flex items-center gap-2">
                <TeamChoice team={a} chosen={!!a && picks[game.index] === a} disabled={false} onPick={() => a && setDraft(withPick(bracket.games, bracket.teams, picks, game.index, a))} />
                <span className="shrink-0 text-xs text-muted-foreground">ou</span>
                <TeamChoice team={b} chosen={!!b && picks[game.index] === b} disabled={false} onPick={() => b && setDraft(withPick(bracket.games, bracket.teams, picks, game.index, b))} />
              </div>
            );
          })}
        </section>
      ))}

      <Button size="lg" disabled={!complete || !dirty || save.isPending} onClick={() => save.mutate()}>
        {bracket.myPicks && !dirty ? "Tableau enregistré" : "Valider mon tableau"}
      </Button>
      {bracket.myPicks && !dirty && champion && (
        <ShareButton path="/coupe" text={`Mon champion de la ${bracket.label} : ${teamLabel(champion)}. Et toi ?`} label="Partager mon champion" className="self-center" />
      )}
    </div>
  );
}

function LockedBracket({ bracket }: { bracket: Bracket }) {
  const teams = useTeamsByAbbreviation();
  const teamLabel = useTeamLabel();
  const total = bracket.champions?.reduce((sum, c) => sum + c.players, 0) ?? 0;

  return (
    <div className="flex flex-col gap-4">
      {bracket.myPoints !== null && (
        <Card>
          <CardContent className="flex items-center justify-between gap-3 pt-5">
            <span className="text-sm font-medium">Ton tableau</span>
            <span className="font-mono text-2xl font-bold">{bracket.myPoints} pts</span>
          </CardContent>
        </Card>
      )}

      {roundsOf(bracket.games).map((round) => (
        <section key={round.round} className="flex flex-col gap-2">
          <h2 className="flex items-baseline justify-between gap-2 font-heading text-base font-bold">
            {round.label}
            <span className="font-mono text-xs font-normal text-muted-foreground">{round.points} pts</span>
          </h2>
          {round.games.map((game) => {
            const mine = bracket.myPicks?.[game.index] ?? null;
            return (
              <div key={game.index} className="glass flex flex-col gap-1.5 rounded-xl px-3 py-2 text-sm">
                <div className="flex items-center gap-2">
                  {[game.teamA, game.teamB].map((team, i) => (
                    <span key={i} className={cn("flex min-w-0 flex-1 items-center gap-1.5", game.winner && team !== game.winner && "opacity-50", game.winner === team && team && "font-bold")}>
                      {team ? <TeamLogo abbreviation={team} logoUrl={teams.get(team)?.logoUrl} size={22} /> : null}
                      <span className="min-w-0 truncate">{team ? teamLabel(team) : "À venir"}</span>
                    </span>
                  ))}
                </div>
                {mine && (
                  <p className="flex items-center gap-1 text-xs text-muted-foreground">
                    {game.winner && (mine === game.winner ? <Check className="h-3.5 w-3.5 text-success" /> : <X className="h-3.5 w-3.5 text-destructive" />)}
                    Ton choix : {teamLabel(mine)}
                  </p>
                )}
              </div>
            );
          })}
        </section>
      ))}

      {bracket.champions && bracket.champions.length > 0 && (
        <section className="flex flex-col gap-2">
          <h2 className="font-heading text-base font-bold">Champions choisis</h2>
          {bracket.champions.map((c) => {
            const share = Math.round((c.players * 100) / Math.max(1, total));
            return (
              <div key={c.team} className="glass-inset-quiet relative flex items-center gap-2 overflow-hidden rounded-xl px-3 py-2 text-sm font-semibold">
                <span aria-hidden className="absolute inset-y-0 left-0 bg-primary/20" style={{ width: `${share}%` }} />
                <TeamLogo abbreviation={c.team} logoUrl={teams.get(c.team)?.logoUrl} size={22} className="relative" />
                <span className="relative min-w-0 flex-1 truncate">{teamLabel(c.team)}</span>
                <span className="relative shrink-0 font-mono">{share} %</span>
              </div>
            );
          })}
        </section>
      )}

      {bracket.ranking && bracket.ranking.length > 0 && (
        <section className="flex flex-col gap-1.5">
          <h2 className="font-heading text-base font-bold">Classement du tableau</h2>
          {bracket.ranking.map((e) => (
            <Link key={e.username} href={`/u/${encodeURIComponent(e.username)}`} className={cn("glass flex items-center gap-2.5 rounded-xl px-3 py-2 text-sm", e.me && "ring-1 ring-primary")}>
              <span className="w-6 shrink-0 text-center font-mono text-xs font-bold text-muted-foreground">{e.rank}</span>
              <PlayerAvatar number={e.avatarNumber} position={e.avatarPosition} colorway={e.avatarColorway} icon={e.avatarIcon} size="xs" />
              <span className="min-w-0 flex-1 truncate font-medium">{e.username}</span>
              <TeamLogo abbreviation={e.champion} logoUrl={teams.get(e.champion)?.logoUrl} size={20} />
              <span className="w-14 shrink-0 text-right font-mono font-bold">{e.points} pts</span>
            </Link>
          ))}
        </section>
      )}
    </div>
  );
}
