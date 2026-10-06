"use client";

import { useState } from "react";
import { GitCompareArrows, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { TeamLogo } from "@/components/team-logo";
import { InjuryPill, PlayerHeadshot, playerName } from "@/components/players/player-tile";
import { RosterPlayer } from "@/lib/types";
import { cn } from "@/lib/utils";

// lowerIsBetter : pour les balles perdues, la meilleure valeur est la plus basse.
const ROWS: { label: string; value: (p: RosterPlayer) => number | null; unit?: string; lowerIsBetter?: boolean }[] = [
  { label: "Points", value: (p) => p.pointsPerGame },
  { label: "Rebonds", value: (p) => p.reboundsPerGame },
  { label: "Passes", value: (p) => p.assistsPerGame },
  { label: "Interceptions", value: (p) => p.stealsPerGame },
  { label: "Contres", value: (p) => p.blocksPerGame },
  { label: "Balles perdues", value: (p) => p.turnoversPerGame, lowerIsBetter: true },
  { label: "Minutes", value: (p) => p.minutesPerGame },
  { label: "Tirs", value: (p) => p.fieldGoalPct, unit: "%" },
  { label: "3 points", value: (p) => p.threePointPct, unit: "%" },
  { label: "Lancers francs", value: (p) => p.freeThrowPct, unit: "%" },
  { label: "Matchs joués", value: (p) => p.gamesPlayed },
];

function format(value: number | null, unit?: string) {
  if (value === null) return "-";
  return `${Number.isInteger(value) ? value : value.toFixed(1)}${unit ?? ""}`;
}

function CompareTable({ a, b }: { a: RosterPlayer; b: RosterPlayer }) {
  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-3">
        {[a, b].map((p, i) => (
          <div key={p.id} className={cn("flex flex-col items-center gap-1.5 text-center", i === 1 && "col-start-3")}>
            <PlayerHeadshot player={p} size={72} />
            <p className="font-heading font-bold leading-tight">{playerName(p)}</p>
            <p className="flex items-center gap-1 text-xs text-muted-foreground">
              {p.team && <TeamLogo abbreviation={p.team.abbreviation} logoUrl={p.team.logoUrl} size={16} />}
              {p.team?.abbreviation} {p.position && `· ${p.position}`}
            </p>
            <InjuryPill status={p.injuryStatus} />
          </div>
        ))}
        <span className="col-start-2 row-start-1 font-heading text-sm font-bold text-muted-foreground">VS</span>
      </div>

      <div className="flex flex-col">
        {ROWS.map((row) => {
          const va = row.value(a);
          const vb = row.value(b);
          // Meilleure valeur en évidence, seulement si les deux existent et diffèrent.
          const aBetter = va !== null && vb !== null && va !== vb && (row.lowerIsBetter ? va < vb : va > vb);
          const bBetter = va !== null && vb !== null && va !== vb && !aBetter;
          return (
            <div
              key={row.label}
              className="grid grid-cols-[1fr_auto_1fr] items-center gap-3 py-1.5 shadow-[inset_0_-1px_0_var(--hairline)] last:shadow-none"
            >
              <span className={cn("text-right font-mono tabular-nums", aBetter ? "font-bold text-success" : "text-muted-foreground")}>
                {format(va, row.unit)}
              </span>
              <span className="w-28 text-center text-xs text-muted-foreground">{row.label}</span>
              <span className={cn("font-mono tabular-nums", bBetter ? "font-bold text-success" : "text-muted-foreground")}>
                {format(vb, row.unit)}
              </span>
            </div>
          );
        })}
      </div>
      <p className="text-center text-[11px] text-muted-foreground">
        Moyennes par match, saison {a.statsSeasonLabel ?? b.statsSeasonLabel ?? "en cours"}. En vert : la meilleure des deux.
      </p>
    </div>
  );
}

/**
 * Bandeau du comparateur (bas de l'écran) : les joueurs choisis, et le
 * bouton qui ouvre la comparaison dès qu'il y en a deux.
 */
export function PlayerCompareTray({
  players,
  onRemove,
  onClear,
}: {
  players: RosterPlayer[];
  onRemove: (p: RosterPlayer) => void;
  onClear: () => void;
}) {
  const [open, setOpen] = useState(false);
  if (players.length === 0) return null;

  return (
    <>
      <div
        className="glass-strong fixed inset-x-3 z-40 flex items-center gap-3 rounded-2xl px-3 py-2.5 md:inset-x-auto md:right-6 md:w-[26rem]"
        style={{ bottom: "calc(env(safe-area-inset-bottom, 0px) + 96px)" }}
        role="region"
        aria-label="Comparateur de joueurs"
      >
        <GitCompareArrows className="h-4 w-4 shrink-0 text-primary" />
        <div className="flex min-w-0 flex-1 items-center gap-2">
          {players.map((p) => (
            <span key={p.id} className="glass-inset-quiet flex min-w-0 items-center gap-1.5 rounded-full py-0.5 pl-0.5 pr-2 text-xs">
              <PlayerHeadshot player={p} size={22} />
              <span className="truncate">{p.lastName}</span>
              <button type="button" onClick={() => onRemove(p)} aria-label={`Retirer ${playerName(p)}`}>
                <X className="h-3 w-3 text-muted-foreground hover:text-foreground" />
              </button>
            </span>
          ))}
          {players.length === 1 && <span className="truncate text-xs text-muted-foreground">Choisis un 2e joueur</span>}
        </div>
        <Button size="sm" disabled={players.length < 2} onClick={() => setOpen(true)}>
          Comparer
        </Button>
        <button type="button" onClick={onClear} aria-label="Vider le comparateur" className="text-muted-foreground hover:text-foreground">
          <X className="h-4 w-4" />
        </button>
      </div>

      <Dialog open={open && players.length === 2} onOpenChange={setOpen}>
        <DialogContent className="max-h-[90vh] max-w-lg overflow-y-auto">
          <DialogTitle>Comparateur</DialogTitle>
          <DialogDescription className="sr-only">Statistiques des deux joueurs côte à côte.</DialogDescription>
          {players.length === 2 && <CompareTable a={players[0]} b={players[1]} />}
        </DialogContent>
      </Dialog>
    </>
  );
}
