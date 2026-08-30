"use client";

import { useQuery } from "@tanstack/react-query";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchPlayerRecentGames } from "@/lib/api/players";
import { RosterPlayer } from "@/lib/types";
import { cn } from "@/lib/utils";

function StatCell({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col items-center gap-0.5 rounded-lg bg-white/5 px-2 py-2 text-center">
      <span className="font-mono text-base font-bold">{value}</span>
      <span className="text-[10px] uppercase tracking-wide text-muted-foreground">{label}</span>
    </div>
  );
}

const INJURY_BADGE_STYLE: Record<string, string> = {
  Out: "border-destructive/40 text-destructive",
  "Day-To-Day": "border-amber-500/40 text-amber-500",
};

function RecentForm({ playerId }: { playerId: string }) {
  // Appel ESPN en direct à l'ouverture de la carte, pas de pré-synchro (voir
  // PlayerController.recentGames côté backend) : d'où le chargement notable
  // ici, contrairement aux moyennes saison déjà en base.
  const { data, isLoading, isError } = useQuery({
    queryKey: ["player-recent-games", playerId],
    queryFn: () => fetchPlayerRecentGames(playerId),
    staleTime: 5 * 60 * 1000,
  });

  if (isLoading) {
    return (
      <div className="mt-4 flex flex-col gap-1.5">
        <Skeleton className="h-4 w-32" />
        <Skeleton className="h-6 w-full" />
        <Skeleton className="h-6 w-full" />
      </div>
    );
  }

  if (isError || !data || data.length === 0) return null;

  return (
    <div className="mt-4">
      <p className="mb-2 text-xs text-muted-foreground">Forme récente (5 derniers matchs)</p>
      <ul className="flex flex-col gap-1">
        {data.map((g) => (
          <li
            key={g.date}
            className="flex items-center justify-between gap-2 rounded-lg bg-white/5 px-2.5 py-1.5 text-xs"
          >
            <span className="flex items-center gap-1.5">
              <span
                className={cn(
                  "font-mono font-bold",
                  g.result === "W" ? "text-emerald-500" : g.result === "L" ? "text-destructive" : "text-muted-foreground"
                )}
              >
                {g.result ?? "?"}
              </span>
              <span className="text-muted-foreground">vs {g.opponentAbbreviation ?? "?"}</span>
            </span>
            <span className="font-mono">
              {g.points} pts · {g.rebounds} reb · {g.assists} pd
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

export function PlayerCardDialog({
  player,
  onOpenChange,
}: {
  player: RosterPlayer | null;
  onOpenChange: (open: boolean) => void;
}) {
  return (
    <Dialog open={player !== null} onOpenChange={onOpenChange}>
      {player && (
        <DialogContent className="max-w-md">
          <div className="flex items-center gap-4">
            {player.headshotUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={player.headshotUrl}
                alt={`${player.firstName} ${player.lastName}`}
                className="h-16 w-16 shrink-0 rounded-full bg-white/10 object-cover"
              />
            ) : (
              <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-white/10 font-mono text-lg font-bold text-muted-foreground">
                {player.jersey ?? "?"}
              </div>
            )}
            <div className="min-w-0 flex-1">
              <DialogTitle className="truncate">
                {player.firstName} {player.lastName}
              </DialogTitle>
              <p className="mt-0.5 text-xs text-muted-foreground">
                {[
                  player.jersey && `#${player.jersey}`,
                  player.position,
                  player.height,
                  player.weight,
                ]
                  .filter(Boolean)
                  .join(" · ")}
              </p>
            </div>
          </div>

          {player.injuryStatus && (
            <Badge
              variant="outline"
              className={cn("mt-3 w-fit", INJURY_BADGE_STYLE[player.injuryStatus] ?? "text-muted-foreground")}
            >
              {player.injuryStatus}
            </Badge>
          )}

          {player.pointsPerGame !== null ? (
            <div className="mt-4">
              <p className="mb-2 text-xs text-muted-foreground">
                Moyennes saison {player.statsSeasonLabel && `(${player.statsSeasonLabel})`}
                {player.gamesPlayed !== null && ` · ${player.gamesPlayed} matchs joués`}
              </p>
              <div className="grid grid-cols-4 gap-2">
                <StatCell label="Pts" value={player.pointsPerGame.toFixed(1)} />
                <StatCell label="Reb" value={player.reboundsPerGame?.toFixed(1) ?? "-"} />
                <StatCell label="Pd" value={player.assistsPerGame?.toFixed(1) ?? "-"} />
                <StatCell label="Min" value={player.minutesPerGame?.toFixed(1) ?? "-"} />
                <StatCell label="Int" value={player.stealsPerGame?.toFixed(1) ?? "-"} />
                <StatCell label="Ctr" value={player.blocksPerGame?.toFixed(1) ?? "-"} />
                <StatCell label="Perte" value={player.turnoversPerGame?.toFixed(1) ?? "-"} />
                <StatCell
                  label="%Tirs"
                  value={player.fieldGoalPct !== null ? `${player.fieldGoalPct.toFixed(0)}%` : "-"}
                />
              </div>
            </div>
          ) : (
            <p className="mt-4 text-sm text-muted-foreground">
              Pas encore de statistiques pour cette saison.
            </p>
          )}

          <RecentForm playerId={player.id} />
        </DialogContent>
      )}
    </Dialog>
  );
}
