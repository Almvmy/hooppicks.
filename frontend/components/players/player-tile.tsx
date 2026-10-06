import { GitCompareArrows, Check } from "lucide-react";
import { TeamLogo } from "@/components/team-logo";
import { getTeamColor } from "@/lib/team-colors";
import { RosterPlayer } from "@/lib/types";
import { cn } from "@/lib/utils";

export function playerName(p: Pick<RosterPlayer, "firstName" | "lastName">): string {
  return [p.firstName, p.lastName].filter(Boolean).join(" ");
}

export function PlayerHeadshot({ player, size }: { player: RosterPlayer; size: number }) {
  if (player.headshotUrl) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={player.headshotUrl}
        alt={playerName(player)}
        width={size}
        height={size}
        loading="lazy"
        className="shrink-0 rounded-full bg-tint/10 object-cover"
        style={{ width: size, height: size }}
      />
    );
  }
  return (
    <span
      aria-hidden
      className="shrink-0 rounded-full"
      style={{ width: size, height: size, backgroundColor: player.team ? getTeamColor(player.team.abbreviation) : "#6B7280" }}
    />
  );
}

/** Pastille de blessure : "Out" en rouge (absent), le reste (Day-To-Day…) en ambre (incertain). */
export function InjuryPill({ status }: { status: string | null }) {
  if (!status) return null;
  const out = status.toLowerCase() === "out";
  return (
    <span
      className={cn(
        "shrink-0 rounded-full px-2 py-0.5 text-[10px] font-bold uppercase",
        out ? "bg-destructive/15 text-destructive" : "bg-amber-500/15 text-amber-500 light:text-amber-800"
      )}
    >
      {out ? "Absent" : status}
    </span>
  );
}

function Stat({ value, label }: { value: number | null; label: string }) {
  return (
    <span className="flex flex-col items-center">
      <span className="font-mono text-sm font-bold tabular-nums">{value === null ? "-" : value.toFixed(1)}</span>
      <span className="text-[10px] uppercase text-muted-foreground">{label}</span>
    </span>
  );
}

/**
 * Carte joueur de liste : photo, équipe, poste, moyennes saison et blessure.
 * onCompare (facultatif) ajoute le bouton du comparateur.
 */
export function PlayerTile({
  player,
  onSelect,
  onCompare,
  inComparison = false,
}: {
  player: RosterPlayer;
  onSelect: (p: RosterPlayer) => void;
  onCompare?: (p: RosterPlayer) => void;
  inComparison?: boolean;
}) {
  return (
    <div className={cn("glass relative flex flex-col gap-3 rounded-2xl p-4", inComparison && "ring-2 ring-primary/60")}>
      <button type="button" onClick={() => onSelect(player)} className="flex items-center gap-3 text-left">
        <PlayerHeadshot player={player} size={48} />
        <div className="min-w-0 flex-1">
          <p className="truncate font-medium">{playerName(player)}</p>
          <p className="flex items-center gap-1.5 truncate text-xs text-muted-foreground">
            {player.team && <TeamLogo abbreviation={player.team.abbreviation} logoUrl={player.team.logoUrl} size={16} />}
            {player.team?.name ?? "Sans équipe"}
            {player.position && <span className="font-mono font-bold text-primary">· {player.position}</span>}
            {player.jersey && <span className="font-mono">#{player.jersey}</span>}
          </p>
        </div>
        <InjuryPill status={player.injuryStatus} />
      </button>

      <div className="flex items-center justify-between gap-2">
        <div className="grid flex-1 grid-cols-3">
          <Stat value={player.pointsPerGame} label="pts" />
          <Stat value={player.reboundsPerGame} label="reb" />
          <Stat value={player.assistsPerGame} label="pd" />
        </div>
        {onCompare && (
          <button
            type="button"
            onClick={() => onCompare(player)}
            aria-pressed={inComparison}
            aria-label={inComparison ? `Retirer ${playerName(player)} du comparateur` : `Comparer ${playerName(player)}`}
            title={inComparison ? "Retirer du comparateur" : "Comparer"}
            className={cn(
              "flex h-8 w-8 shrink-0 items-center justify-center rounded-full transition-colors",
              inComparison ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
            )}
          >
            {inComparison ? <Check className="h-4 w-4" /> : <GitCompareArrows className="h-4 w-4" />}
          </button>
        )}
      </div>
    </div>
  );
}
