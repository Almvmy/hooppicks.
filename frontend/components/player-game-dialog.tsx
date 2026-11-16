"use client";

import { Trophy } from "lucide-react";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import { ShotCourt } from "@/components/match-play-sections";
import { BigStat, PlayerHero, SectionLabel, ShootingBar, SmallStat, positionLabel, teamColor } from "@/components/player-hero";
import { bestRated, playerRating, ratingClass } from "@/lib/player-rating";
import type { LiveShot } from "@/lib/live";
import type { PlayerBoxScore, RosterPlayer, Team } from "@/lib/types";
import { cn } from "@/lib/utils";

function ratingWord(rating: number): string {
  if (rating >= 9) return "Exceptionnel";
  if (rating >= 8) return "Excellent";
  if (rating >= 7) return "Très bon";
  if (rating >= 6) return "Correct";
  return "Discret";
}

const split = (s: string) => {
  const [made, attempted] = s.split("-").map((n) => Number.parseInt(n, 10) || 0);
  return { made, attempted, pct: attempted ? (made / attempted) * 100 : null, detail: `${made}/${attempted}` };
};

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
    !!player && teammates.length > 1 && stat(player) > 0 && teammates.every((p) => stat(p) <= stat(player))
      ? "1er de l'équipe"
      : null;
  const manOfTheMatch = !!player && bestRated(matchPlayers ?? [])?.player.playerName === player.playerName;
  const color = teamColor(team);

  return (
    <Dialog open={player !== null} onOpenChange={onOpenChange}>
      {player && team && (
        <DialogContent className="max-h-[90vh] w-[calc(100%-1.5rem)] max-w-md overflow-y-auto p-0">
          <PlayerHero
            name={player.playerName}
            team={team}
            jersey={info?.jersey}
            headshotUrl={info?.headshotUrl}
            details={[info?.jersey && `#${info.jersey}`, positionLabel(info?.position)]}
            chips={
              <>
                {player.starter && <span className="rounded-full bg-tint/10 px-2 py-0.5 text-[10px] font-semibold">Titulaire</span>}
                {manOfTheMatch && (
                  <span className="flex items-center gap-1 rounded-full bg-brand px-2 py-0.5 text-[10px] font-bold text-brand-foreground">
                    <Trophy className="h-3 w-3" />
                    Homme du match
                  </span>
                )}
              </>
            }
          />

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
              <BigStat label="Points" value={player.points} note={leads((p) => p.points)} />
              <BigStat label="Rebonds" value={player.rebounds} note={leads((p) => p.rebounds)} />
              <BigStat label="Passes" value={player.assists} note={leads((p) => p.assists)} />
            </div>

            <div className="grid grid-cols-5 gap-1.5">
              <SmallStat label="Min" value={Number.parseInt(player.minutes, 10) || 0} />
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
              <SectionLabel>Adresse</SectionLabel>
              {[
                ["Tirs", player.fieldGoals],
                ["À 3 points", player.threePoints],
                ["Lancers francs", player.freeThrows],
              ].map(([label, value]) => {
                const s = split(value);
                return <ShootingBar key={label} label={label} pct={s.pct} detail={s.detail} color={color} />;
              })}
            </div>

            {own.length > 0 && (
              <div className="flex flex-col gap-2">
                <SectionLabel>Ses tirs</SectionLabel>
                <ShotCourt shots={own} color={color} />
              </div>
            )}
          </div>
        </DialogContent>
      )}
    </Dialog>
  );
}
