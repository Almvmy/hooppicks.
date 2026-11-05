"use client";

import { useState } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronRight, ListOrdered, Trophy } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { CupBracket, EMPTY_SLOTS, type BracketSlot, type BracketSlots, type SlotState } from "@/components/cup-bracket";
import { PlayerAvatar } from "@/components/player-avatar";
import { ShareButton } from "@/components/share-button";
import { TeamLogo } from "@/components/team-logo";
import { fetchBracket, saveBracketPicks, withPick, type Bracket } from "@/lib/api/bracket";
import { useTeamsByAbbreviation } from "@/lib/use-teams";
import { cn, formatShortDateTime } from "@/lib/utils";

// Ordre du tableau (BracketTree) : quarts 0 et 1 à l'Est, 2 et 3 à l'Ouest, demies 4 (Est) et 5 (Ouest), finale 6.
const EAST_QUARTERS = [0, 1];
const WEST_QUARTERS = [2, 3];
const EAST_SEMI = 4;
const WEST_SEMI = 5;
const FINAL = 6;

function CupLinks() {
  return (
    <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
      <Link href="/players?tab=equipes&vue=coupe" className="glass flex items-center gap-2 rounded-xl px-4 py-3 text-sm font-semibold hover:text-primary">
        <ListOrdered className="h-4 w-4 shrink-0 text-primary" />
        <span className="flex-1">Classement des poules</span>
        <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" />
      </Link>
      <Link href="/leaderboard?periode=cup" className="glass flex items-center gap-2 rounded-xl px-4 py-3 text-sm font-semibold hover:text-primary">
        <Trophy className="h-4 w-4 shrink-0 text-primary" />
        <span className="flex-1">Classement des pronostiqueurs</span>
        <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" />
      </Link>
    </div>
  );
}

export default function CoupePage() {
  const query = useQuery({ queryKey: ["bracket"], queryFn: fetchBracket });

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-4">
      <h1 className="font-heading text-2xl font-bold">{query.data?.label ?? "Coupe NBA"}</h1>
      {query.isLoading && <Skeleton className="aspect-[600/470] w-full rounded-2xl" />}
      {query.isError && <p className="text-sm text-destructive">Impossible de charger le tableau.</p>}
      {query.data === null && (
        <>
          <CupBracket slots={EMPTY_SLOTS} />
          <p className="text-center text-sm font-semibold">Le tableau ouvre à la fin des poules</p>
        </>
      )}
      {query.data && (query.data.locked ? <LockedBracket bracket={query.data} /> : <OpenBracket key={query.data.id} bracket={query.data} />)}
      <CupLinks />
    </div>
  );
}

function OpenBracket({ bracket }: { bracket: Bracket }) {
  const queryClient = useQueryClient();
  const teams = useTeamsByAbbreviation();
  const [draft, setDraft] = useState<(string | null)[] | null>(null);
  const picks = draft ?? bracket.myPicks ?? bracket.games.map(() => null);
  const complete = picks.every(Boolean);
  const dirty = draft !== null;
  const champion = picks[FINAL] ?? null;
  const name = (team: string) => teams.get(team)?.name ?? team;

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

  const pick = (game: number, team: string) => setDraft(withPick(bracket.games, bracket.teams, picks, game, team));

  // Une case propose son équipe pour le match suivant ; une fois ce match choisi, l'autre s'éteint.
  function slot(team: string | null, game: number): BracketSlot {
    const chosen = picks[game];
    const state: SlotState = !team || !chosen ? "normal" : chosen === team ? (game === FINAL ? "champion" : "chosen") : "out";
    return { team, state, onPick: team ? () => pick(game, team) : undefined };
  }

  const quarterSlots = (games: number[]) =>
    games.flatMap((g) => [slot(bracket.games[g].teamA, g), slot(bracket.games[g].teamB, g)]);

  const slots: BracketSlots = {
    westQuarters: quarterSlots(WEST_QUARTERS),
    eastQuarters: quarterSlots(EAST_QUARTERS),
    westSemis: WEST_QUARTERS.map((g) => slot(picks[g] ?? null, WEST_SEMI)),
    eastSemis: EAST_QUARTERS.map((g) => slot(picks[g] ?? null, EAST_SEMI)),
    westFinalist: slot(picks[WEST_SEMI] ?? null, FINAL),
    eastFinalist: slot(picks[EAST_SEMI] ?? null, FINAL),
  };

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-muted-foreground">
        Touche une équipe pour la qualifier · avant le {formatShortDateTime(new Date(bracket.locksAt))}
      </p>
      <CupBracket slots={slots} />
      <p className="flex flex-wrap justify-between gap-x-4 gap-y-1 text-xs text-muted-foreground">
        <span>Quart 10 pts · Demie 20 pts · Finale 40 pts</span>
        <span>
          {bracket.players} tableau{bracket.players > 1 ? "x" : ""} rempli{bracket.players > 1 ? "s" : ""}
        </span>
      </p>
      {champion && <p className="text-center font-heading text-lg font-bold">Ton champion : {name(champion)}</p>}
      <Button size="lg" disabled={!complete || !dirty || save.isPending} onClick={() => save.mutate()}>
        {bracket.myPicks && !dirty ? "Tableau enregistré" : "Valider mon tableau"}
      </Button>
      {bracket.myPicks && !dirty && champion && (
        <ShareButton path="/coupe" text={`Mon champion de la ${bracket.label} : ${name(champion)}. Et toi ?`} label="Partager mon champion" className="self-center" />
      )}
    </div>
  );
}

function LockedBracket({ bracket }: { bracket: Bracket }) {
  const teams = useTeamsByAbbreviation();
  const name = (team: string) => teams.get(team)?.name ?? team;
  const winners = bracket.games.map((g) => g.winner);
  const mine = bracket.myPicks;
  const total = bracket.champions?.reduce((sum, c) => sum + c.players, 0) ?? 0;

  // Une équipe de quart : éteinte quand elle a perdu.
  function quarter(team: string | null, game: number): BracketSlot {
    return { team, state: winners[game] && winners[game] !== team ? "out" : "normal" };
  }
  // Le vainqueur d'un match avance, cerclé selon ce qu'on avait choisi ; sans résultat, son choix en transparence.
  function advanced(game: number): BracketSlot {
    const winner = winners[game];
    const pick = mine?.[game] ?? null;
    if (!winner) return { team: pick, state: pick ? "ghost" : "normal" };
    const next = game === EAST_SEMI || game === WEST_SEMI ? FINAL : game === EAST_QUARTERS[0] || game === EAST_QUARTERS[1] ? EAST_SEMI : WEST_SEMI;
    const lost = !!winners[next] && winners[next] !== winner;
    const state: SlotState = lost ? "out" : !pick ? "normal" : pick === winner ? "right" : "wrong";
    return { team: winner, state };
  }

  const slots: BracketSlots = {
    westQuarters: WEST_QUARTERS.flatMap((g) => [quarter(bracket.games[g].teamA, g), quarter(bracket.games[g].teamB, g)]),
    eastQuarters: EAST_QUARTERS.flatMap((g) => [quarter(bracket.games[g].teamA, g), quarter(bracket.games[g].teamB, g)]),
    westSemis: WEST_QUARTERS.map(advanced),
    eastSemis: EAST_QUARTERS.map(advanced),
    westFinalist: advanced(WEST_SEMI),
    eastFinalist: advanced(EAST_SEMI),
  };
  if (winners[FINAL]) {
    for (const key of ["westFinalist", "eastFinalist"] as const) {
      if (slots[key].team === winners[FINAL]) slots[key] = { team: winners[FINAL], state: "champion" };
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <CupBracket slots={slots} />

      <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-1 text-sm">
        {winners[FINAL] && <p className="font-heading text-lg font-bold">Champion : {name(winners[FINAL])}</p>}
        {mine?.[FINAL] && <p className="text-muted-foreground">Ton champion : {name(mine[FINAL])}</p>}
      </div>

      {bracket.myPoints !== null && (
        <Card>
          <CardContent className="flex items-center justify-between gap-3 pt-5">
            <span className="text-sm font-medium">Ton tableau</span>
            <span className="font-mono text-2xl font-bold">{bracket.myPoints} pts</span>
          </CardContent>
        </Card>
      )}

      {bracket.champions && bracket.champions.length > 0 && (
        <section className="flex flex-col gap-2">
          <h2 className="font-heading text-base font-bold">Champions choisis</h2>
          {bracket.champions.map((c) => {
            const share = Math.round((c.players * 100) / Math.max(1, total));
            return (
              <div key={c.team} className="glass-inset-quiet relative flex items-center gap-2 overflow-hidden rounded-xl px-3 py-2 text-sm font-semibold">
                <span aria-hidden className="absolute inset-y-0 left-0 bg-primary/20" style={{ width: `${share}%` }} />
                <TeamLogo abbreviation={c.team} logoUrl={teams.get(c.team)?.logoUrl} size={22} className="relative" />
                <span className="relative min-w-0 flex-1 truncate">{name(c.team)}</span>
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
