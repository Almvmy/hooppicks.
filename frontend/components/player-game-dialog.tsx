"use client";

import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { TeamLogo } from "@/components/team-logo";
import { ShotCourt } from "@/components/match-play-sections";
import { getTeamColor } from "@/lib/team-colors";
import { playerRating, ratingClass } from "@/lib/player-rating";
import type { LiveShot } from "@/lib/live";
import type { PlayerBoxScore, RosterPlayer, Team } from "@/lib/types";
import { cn } from "@/lib/utils";

function StatCell({ label, value, strong }: { label: string; value: string | number; strong?: boolean }) {
  return (
    <div className="flex flex-col items-center gap-0.5 rounded-lg bg-tint/5 px-2 py-2 text-center">
      <span className={cn("font-mono text-base", strong ? "font-bold text-primary" : "font-bold")}>{value}</span>
      <span className="text-[10px] uppercase tracking-wide text-muted-foreground">{label}</span>
    </div>
  );
}

const pct = (split: string) => {
  const [made, attempted] = split.split("-").map((n) => Number.parseInt(n, 10) || 0);
  return attempted ? `${Math.round((made / attempted) * 100)} %` : "-";
};

/**
 * Un joueur sur ce match, depuis la feuille de match : toutes ses stats, sa
 * note et ses tirs sur le terrain (si on a le détail du match).
 */
export function PlayerGameDialog({
  player,
  team,
  info,
  shots,
  onOpenChange,
}: {
  player: PlayerBoxScore | null;
  team: Team | null;
  /** Effectif : photo, numéro, poste. */
  info: RosterPlayer | undefined;
  /** Tirs du match (direct ESPN), filtrés ici sur le joueur. */
  shots: LiveShot[] | undefined;
  onOpenChange: (open: boolean) => void;
}) {
  const rating = player ? playerRating(player) : null;
  const own = player && shots ? shots.filter((s) => s.playerName === player.playerName) : [];

  return (
    <Dialog open={player !== null} onOpenChange={onOpenChange}>
      {player && team && (
        <DialogContent className="max-h-[90vh] max-w-md overflow-y-auto">
          <div className="flex items-center gap-3">
            {info?.headshotUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={info.headshotUrl} alt="" className="h-14 w-14 shrink-0 rounded-full bg-tint/10 object-cover" />
            ) : (
              <span className="h-14 w-14 shrink-0 rounded-full bg-tint/10" />
            )}
            <div className="min-w-0 flex-1">
              <DialogTitle className="truncate">{player.playerName}</DialogTitle>
              <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
                <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={16} />
                {team.name}
                {[info?.jersey && `#${info.jersey}`, info?.position].filter(Boolean).map((x) => ` · ${x}`)}
                {player.starter && " · titulaire"}
              </p>
            </div>
            {rating !== null && (
              <span
                className={cn("shrink-0 rounded-lg px-2 py-1 font-mono text-base font-bold", ratingClass(rating))}
                title="Note HoopPicks sur 10"
              >
                {rating.toFixed(1).replace(".", ",")}
              </span>
            )}
          </div>

          <div className="grid grid-cols-4 gap-2">
            <StatCell label="Pts" value={player.points} strong />
            <StatCell label="Reb" value={player.rebounds} />
            <StatCell label="Pd" value={player.assists} />
            <StatCell label="Min" value={Number.parseInt(player.minutes, 10) || 0} />
            <StatCell label="Int" value={player.steals} />
            <StatCell label="Ctr" value={player.blocks} />
            <StatCell label="Bp" value={player.turnovers} />
            <StatCell label="+/-" value={player.plusMinus > 0 ? `+${player.plusMinus}` : player.plusMinus} />
          </div>

          <div className="grid grid-cols-3 gap-2">
            <StatCell label={`Tirs · ${pct(player.fieldGoals)}`} value={player.fieldGoals} />
            <StatCell label={`3 pts · ${pct(player.threePoints)}`} value={player.threePoints} />
            <StatCell label={`LF · ${pct(player.freeThrows)}`} value={player.freeThrows} />
          </div>

          {own.length > 0 && (
            <div className="flex flex-col gap-2">
              <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Ses tirs</p>
              <ShotCourt shots={own} color={getTeamColor(team.abbreviation)} />
            </div>
          )}
        </DialogContent>
      )}
    </Dialog>
  );
}
