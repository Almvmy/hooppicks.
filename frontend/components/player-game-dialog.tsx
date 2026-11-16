"use client";

import { Trophy } from "lucide-react";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { TeamLogo } from "@/components/team-logo";
import { ShotCourt } from "@/components/match-play-sections";
import { getTeamColor } from "@/lib/team-colors";
import { bestRated, playerRating, ratingClass } from "@/lib/player-rating";
import type { LiveShot } from "@/lib/live";
import type { PlayerBoxScore, RosterPlayer, Team } from "@/lib/types";
import { cn } from "@/lib/utils";

const POSITIONS: Record<string, string> = {
  PG: "Meneur",
  SG: "Arrière",
  G: "Arrière",
  SF: "Ailier",
  F: "Ailier",
  PF: "Ailier fort",
  C: "Pivot",
};

function ratingWord(rating: number): string {
  if (rating >= 9) return "Exceptionnel";
  if (rating >= 8) return "Excellent";
  if (rating >= 7) return "Très bon";
  if (rating >= 6) return "Correct";
  return "Discret";
}

const split = (s: string) => {
  const [made, attempted] = s.split("-").map((n) => Number.parseInt(n, 10) || 0);
  return { made, attempted };
};

/** Une statistique majeure ; marquée quand le joueur est le premier de son équipe. */
function BigStat({ label, value, leader }: { label: string; value: number; leader: boolean }) {
  return (
    <div className="flex flex-col items-center gap-0.5">
      <span className={cn("font-heading text-4xl font-bold tabular-nums leading-none", leader && "text-primary")}>{value}</span>
      <span className="text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">{label}</span>
      <span className={cn("text-[10px] font-semibold text-primary", !leader && "invisible")}>1er de l&apos;équipe</span>
    </div>
  );
}

function SmallStat({ label, value, className }: { label: string; value: string | number; className?: string }) {
  return (
    <div className="flex flex-col items-center gap-0.5 rounded-xl bg-tint/5 px-1 py-2">
      <span className={cn("font-mono text-base font-bold tabular-nums", className)}>{value}</span>
      <span className="text-[10px] uppercase tracking-wide text-muted-foreground">{label}</span>
    </div>
  );
}

/** Adresse en barre : réussis / tentés, la barre remplie à hauteur du pourcentage. */
function ShootingBar({ label, value, color }: { label: string; value: string; color: string }) {
  const { made, attempted } = split(value);
  const pct = attempted ? Math.round((made / attempted) * 100) : null;
  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-baseline justify-between text-sm">
        <span className="font-medium">{label}</span>
        <span className="font-mono tabular-nums">
          <span className="font-bold">
            {made}/{attempted}
          </span>
          <span className="ml-2 text-muted-foreground">{pct === null ? "-" : `${pct} %`}</span>
        </span>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-tint/10" aria-hidden>
        <div className="h-full rounded-full" style={{ width: `${pct ?? 0}%`, backgroundColor: color }} />
      </div>
    </div>
  );
}

/**
 * Un joueur sur ce match, depuis la feuille de match : sa note, ses grandes stats (marquées
 * quand il est le premier de son équipe), son adresse et ses tirs sur le terrain (si on a
 * le détail du match). En-tête aux couleurs de son équipe, photo détourée d'ESPN.
 */
export function PlayerGameDialog({
  player,
  team,
  info,
  shots,
  matchPlayers,
  onOpenChange,
}: {
  player: PlayerBoxScore | null;
  team: Team | null;
  /** Effectif : photo, numéro, poste. */
  info: RosterPlayer | undefined;
  /** Tirs du match (direct ESPN), filtrés ici sur le joueur. */
  shots: LiveShot[] | undefined;
  /** Toute la feuille de match : homme du match et premier de son équipe. */
  matchPlayers?: PlayerBoxScore[];
  onOpenChange: (open: boolean) => void;
}) {
  const rating = player ? playerRating(player) : null;
  const own = player && shots ? shots.filter((s) => s.playerName === player.playerName) : [];
  const teammates = player ? (matchPlayers ?? []).filter((p) => p.teamAbbreviation === player.teamAbbreviation) : [];
  const leads = (stat: (p: PlayerBoxScore) => number) =>
    !!player && teammates.length > 1 && stat(player) > 0 && teammates.every((p) => stat(p) <= stat(player));
  const manOfTheMatch = !!player && bestRated(matchPlayers ?? [])?.player.playerName === player.playerName;
  const color = team ? getTeamColor(team.abbreviation) : "#6B7280";
  const minutes = player ? Number.parseInt(player.minutes, 10) || 0 : 0;
  const position = info?.position ? POSITIONS[info.position] ?? info.position : null;

  return (
    <Dialog open={player !== null} onOpenChange={onOpenChange}>
      {player && team && (
        <DialogContent className="max-h-[90vh] w-[calc(100%-1.5rem)] max-w-md overflow-y-auto p-0">
          <div className="relative h-40 overflow-hidden rounded-t-2xl" style={{ background: `radial-gradient(120% 140% at 0% 100%, ${color}80 0%, ${color}26 45%, transparent 75%)` }}>
            {info?.jersey && (
              <span aria-hidden className="absolute -bottom-6 right-2 font-heading text-[150px] font-bold leading-none opacity-[0.08]">
                {info.jersey}
              </span>
            )}
            {info?.headshotUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={info.headshotUrl} alt="" className="absolute bottom-0 left-2 h-36 w-auto max-w-[40%] object-contain object-bottom" />
            ) : (
              <span className="absolute bottom-4 left-5 h-24 w-24 rounded-full bg-tint/10" />
            )}
            <div className="absolute inset-y-0 right-0 flex w-[56%] flex-col justify-center gap-1.5 pr-5">
              <DialogTitle className="pr-6 font-heading text-xl font-bold leading-tight">{player.playerName}</DialogTitle>
              <p className="flex flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
                <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={16} />
                {[team.name, info?.jersey && `#${info.jersey}`, position].filter(Boolean).join(" · ")}
              </p>
              <div className="flex flex-wrap gap-1.5">
                {player.starter && <span className="rounded-full bg-tint/10 px-2 py-0.5 text-[10px] font-semibold">Titulaire</span>}
                {manOfTheMatch && (
                  <span className="flex items-center gap-1 rounded-full bg-brand px-2 py-0.5 text-[10px] font-bold text-brand-foreground">
                    <Trophy className="h-3 w-3" />
                    Homme du match
                  </span>
                )}
              </div>
            </div>
          </div>

          <div className="flex flex-col gap-5 p-5">
            {rating !== null && (
              <div className="flex items-center justify-between gap-3 rounded-2xl bg-tint/5 px-4 py-3">
                <div>
                  <p className="text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">Note HoopPicks</p>
                  <p className="font-heading text-base font-bold">{ratingWord(rating)}</p>
                </div>
                <span className={cn("rounded-xl px-3 py-1.5 font-mono text-2xl font-bold tabular-nums", ratingClass(rating))} title="Note HoopPicks sur 10">
                  {rating.toFixed(1).replace(".", ",")}
                </span>
              </div>
            )}

            <div className="grid grid-cols-3">
              <BigStat label="Points" value={player.points} leader={leads((p) => p.points)} />
              <BigStat label="Rebonds" value={player.rebounds} leader={leads((p) => p.rebounds)} />
              <BigStat label="Passes" value={player.assists} leader={leads((p) => p.assists)} />
            </div>

            <div className="grid grid-cols-5 gap-1.5">
              <SmallStat label="Min" value={minutes} />
              <SmallStat label="Int" value={player.steals} />
              <SmallStat label="Ctr" value={player.blocks} />
              <SmallStat label="BP" value={player.turnovers} />
              <SmallStat
                label="+/-"
                value={player.plusMinus > 0 ? `+${player.plusMinus}` : player.plusMinus}
                className={player.plusMinus > 0 ? "text-success" : player.plusMinus < 0 ? "text-destructive" : undefined}
              />
            </div>

            <div className="flex flex-col gap-3">
              <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Adresse</p>
              <ShootingBar label="Tirs" value={player.fieldGoals} color={color} />
              <ShootingBar label="À 3 points" value={player.threePoints} color={color} />
              <ShootingBar label="Lancers francs" value={player.freeThrows} color={color} />
            </div>

            {own.length > 0 && (
              <div className="flex flex-col gap-2">
                <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Ses tirs</p>
                <ShotCourt shots={own} color={color} />
              </div>
            )}
          </div>
        </DialogContent>
      )}
    </Dialog>
  );
}
