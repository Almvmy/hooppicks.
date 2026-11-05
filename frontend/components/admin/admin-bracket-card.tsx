"use client";

import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { GitFork } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { fetchBracket, publishBracket, roundLabel, setBracketWinner, type Bracket } from "@/lib/api/bracket";
import { useTeamsByAbbreviation } from "@/lib/use-teams";

/** Ordre du tableau : 1 contre 2, 3 contre 4 (les deux à l'Est) ; 5 contre 6, 7 contre 8 (à l'Ouest). */
const QUARTERS = ["Quart Est 1", "Quart Est 2", "Quart Ouest 1", "Quart Ouest 2"];

function toLocalInput(iso: string): string {
  return iso.slice(0, 16);
}

/**
 * Le tableau à remplir : les 8 équipes une fois les poules finies, l'heure du
 * premier quart. Les vainqueurs arrivent tout seuls depuis les matchs ; à saisir ici
 * si la source tarde ou se trompe.
 */
export function AdminBracketCard() {
  const { data, isLoading, isError } = useQuery({ queryKey: ["bracket"], queryFn: fetchBracket });

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-6">
        <p className="flex items-center gap-2 text-sm font-medium">
          <GitFork className="h-4 w-4 text-primary" />
          Tableau à remplir
        </p>
        {isLoading && <p className="text-sm text-muted-foreground">Chargement…</p>}
        {isError && <p className="text-sm text-destructive">Impossible de charger le tableau.</p>}
        {data !== undefined && !isError && <BracketForm key={data?.id ?? "nouveau"} bracket={data} />}
        {data && <Winners bracket={data} />}
      </CardContent>
    </Card>
  );
}

function BracketForm({ bracket }: { bracket: Bracket | null }) {
  const queryClient = useQueryClient();
  const teams = [...useTeamsByAbbreviation().values()].sort((a, b) => a.name.localeCompare(b.name));
  const [year] = useState(() => new Date().getFullYear());
  const [label, setLabel] = useState(bracket?.label ?? `Coupe NBA ${year}`);
  const [slots, setSlots] = useState<string[]>(() => {
    if (!bracket) return Array(8).fill("");
    return bracket.games.filter((g) => g.round === 0).flatMap((g) => [g.teamA ?? "", g.teamB ?? ""]);
  });
  const [locksAt, setLocksAt] = useState(bracket ? toLocalInput(bracket.locksAt) : "");

  const publish = useMutation({
    mutationFn: () => publishBracket({ id: bracket?.id ?? `cup-${year}`, label, teams: slots, locksAt: `${locksAt}:00Z` }),
    onSuccess: () => {
      toast.success("Tableau publié.");
      queryClient.invalidateQueries({ queryKey: ["bracket"] });
    },
    onError: (e: Error) => toast.error(e.message || "Publication impossible."),
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    publish.mutate();
  }

  function select(slot: number) {
    return (
      <select
        aria-label={`Équipe ${slot + 1}`}
        value={slots[slot]}
        onChange={(e) => setSlots((s) => s.map((v, i) => (i === slot ? e.target.value : v)))}
        className="glass-inset min-w-0 flex-1 rounded-lg px-2 py-1.5 text-sm"
      >
        <option value="">Équipe…</option>
        {teams.map((t) => (
          <option key={t.abbreviation} value={t.abbreviation}>
            {t.name}
          </option>
        ))}
      </select>
    );
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-2">
      <Input value={label} onChange={(e) => setLabel(e.target.value)} placeholder="Nom" aria-label="Nom du tableau" className="max-w-xs" />
      {QUARTERS.map((name, q) => (
        <div key={name} className="flex flex-wrap items-center gap-2">
          <span className="w-24 shrink-0 text-xs text-muted-foreground">{name}</span>
          {select(q * 2)}
          {select(q * 2 + 1)}
        </div>
      ))}
      <label className="flex flex-wrap items-center gap-2 text-sm">
        <span className="text-xs text-muted-foreground">Premier quart (GMT)</span>
        <input type="datetime-local" value={locksAt} onChange={(e) => setLocksAt(e.target.value)} className="glass-inset rounded-lg px-2 py-1.5 text-sm" />
      </label>
      <Button type="submit" size="sm" className="self-start" disabled={publish.isPending || !locksAt || slots.some((s) => !s)}>
        {bracket ? "Enregistrer" : "Publier et prévenir les joueurs"}
      </Button>
    </form>
  );
}

function Winners({ bracket }: { bracket: Bracket }) {
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: ({ game, team }: { game: number; team: string | null }) => setBracketWinner(game, team),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["bracket"] }),
    onError: (e: Error) => toast.error(e.message || "Enregistrement impossible."),
  });
  if (!bracket.locked) return null;

  return (
    <div className="flex flex-col gap-1.5 border-t border-tint/10 pt-3">
      <p className="text-xs font-medium text-muted-foreground">Vainqueurs</p>
      {bracket.games
        .filter((g) => g.teamA && g.teamB)
        .map((g) => (
          <div key={g.index} className="flex flex-wrap items-center gap-2 text-sm">
            <span className="w-28 shrink-0 text-xs text-muted-foreground">{roundLabel(bracket.games, g.round)}</span>
            {[g.teamA, g.teamB].map((team) => (
              <Button key={team} size="sm" variant={g.winner === team ? "lit" : "outline"} disabled={save.isPending} onClick={() => save.mutate({ game: g.index, team })}>
                {team}
              </Button>
            ))}
          </div>
        ))}
    </div>
  );
}
