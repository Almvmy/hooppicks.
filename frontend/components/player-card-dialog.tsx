"use client";

import { useQuery } from "@tanstack/react-query";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { BigStat, PlayerHero, SectionLabel, ShootingBar, SmallStat, positionLabel, teamColor } from "@/components/player-hero";
import { fetchPlayerRecentGames } from "@/lib/api/players";
import { RosterPlayer } from "@/lib/types";
import { cn } from "@/lib/utils";

// Statuts de blessure d'ESPN, en français.
const INJURY: Record<string, { label: string; className: string }> = {
  Out: { label: "Absent", className: "bg-destructive text-white" },
  Doubtful: { label: "Douteux", className: "bg-destructive/80 text-white" },
  Questionable: { label: "Incertain", className: "bg-amber-500 text-white" },
  "Day-To-Day": { label: "Incertain", className: "bg-amber-500 text-white" },
};

/** « 6' 5" » → « 1,96 m » ; « 184 lbs » → « 83 kg ». ESPN donne les mesures américaines. */
function metricHeight(height: string | null): string | null {
  const m = height?.match(/(\d+)'\s*(\d+)?/);
  if (!m) return height;
  const cm = (Number(m[1]) * 12 + Number(m[2] ?? 0)) * 2.54;
  return `${(cm / 100).toFixed(2).replace(".", ",")} m`;
}

function metricWeight(weight: string | null): string | null {
  const m = weight?.match(/(\d+)/);
  return m ? `${Math.round(Number(m[1]) * 0.4536)} kg` : weight;
}

const one = (n: number | null | undefined) => (n == null ? "-" : n.toLocaleString("fr-FR", { minimumFractionDigits: 1, maximumFractionDigits: 1 }));

function RecentForm({ playerId, color }: { playerId: string; color: string }) {
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
      <div className="flex flex-col gap-1.5">
        <Skeleton className="h-4 w-32" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
      </div>
    );
  }
  if (isError || !data || data.length === 0) return null;
  const best = Math.max(...data.map((g) => g.points), 1);

  return (
    <div className="flex flex-col gap-2">
      <SectionLabel>Ses 5 derniers matchs</SectionLabel>
      <ul className="flex flex-col gap-1.5">
        {data.map((g) => (
          <li key={g.date} className="flex items-center gap-2.5 rounded-xl bg-tint/5 px-3 py-2 text-xs">
            <span
              className={cn(
                "flex h-6 w-6 shrink-0 items-center justify-center rounded-md font-mono text-[11px] font-bold",
                g.result === "W" ? "bg-success/15 text-success" : g.result === "L" ? "bg-destructive/15 text-destructive" : "bg-tint/10 text-muted-foreground"
              )}
              aria-label={g.result === "W" ? "Victoire" : g.result === "L" ? "Défaite" : undefined}
            >
              {g.result === "W" ? "V" : g.result === "L" ? "D" : "?"}
            </span>
            <span className="w-16 shrink-0 text-muted-foreground">vs {g.opponentAbbreviation ?? "?"}</span>
            <span className="flex min-w-0 flex-1 items-center gap-2">
              <span className="h-1.5 flex-1 overflow-hidden rounded-full bg-tint/10" aria-hidden>
                <span className="block h-full rounded-full" style={{ width: `${(g.points / best) * 100}%`, backgroundColor: color }} />
              </span>
              <span className="w-12 shrink-0 text-right font-mono text-sm font-bold tabular-nums">{g.points} pts</span>
            </span>
            <span className="w-16 shrink-0 text-right font-mono text-muted-foreground">
              {g.rebounds} r · {g.assists} p
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

/** La fiche d'un joueur : en-tête aux couleurs de son équipe, moyennes de la saison, adresse, forme récente. */
export function PlayerCardDialog({
  player,
  onOpenChange,
}: {
  player: RosterPlayer | null;
  onOpenChange: (open: boolean) => void;
}) {
  const color = teamColor(player?.team);
  const injury = player?.injuryStatus ? INJURY[player.injuryStatus] ?? { label: player.injuryStatus, className: "bg-tint/10" } : null;

  return (
    <Dialog open={player !== null} onOpenChange={onOpenChange}>
      {player && (
        <DialogContent className="max-h-[90vh] w-[calc(100%-1.5rem)] max-w-md overflow-y-auto p-0">
          <PlayerHero
            name={`${player.firstName ?? ""} ${player.lastName ?? ""}`.trim()}
            team={player.team}
            jersey={player.jersey}
            headshotUrl={player.headshotUrl}
            details={[player.jersey && `#${player.jersey}`, positionLabel(player.position), metricHeight(player.height), metricWeight(player.weight)]}
            chips={injury && <span className={cn("rounded-full px-2 py-0.5 text-[10px] font-bold", injury.className)}>{injury.label}</span>}
          />

          <div className="flex flex-col gap-5 p-5">
            {player.pointsPerGame !== null ? (
              <>
                <div className="flex flex-col gap-3">
                  <SectionLabel>
                    Moyennes {player.statsSeasonLabel ?? "de la saison"}
                    {player.gamesPlayed !== null && ` · ${player.gamesPlayed} match${player.gamesPlayed > 1 ? "s" : ""}`}
                  </SectionLabel>
                  <div className="grid grid-cols-3">
                    <BigStat label="Points" value={one(player.pointsPerGame)} />
                    <BigStat label="Rebonds" value={one(player.reboundsPerGame)} />
                    <BigStat label="Passes" value={one(player.assistsPerGame)} />
                  </div>
                </div>
                <div className="grid grid-cols-4 gap-1.5">
                  <SmallStat label="Min" value={one(player.minutesPerGame)} />
                  <SmallStat label="Int" value={one(player.stealsPerGame)} />
                  <SmallStat label="Ctr" value={one(player.blocksPerGame)} />
                  <SmallStat label="BP" value={one(player.turnoversPerGame)} />
                </div>
                <div className="flex flex-col gap-3">
                  <SectionLabel>Adresse</SectionLabel>
                  <ShootingBar label="Tirs" pct={player.fieldGoalPct} color={color} />
                  <ShootingBar
                    label="À 3 points"
                    pct={player.threePointPct}
                    detail={player.threePointersMadePerGame != null ? `${one(player.threePointersMadePerGame)} par match` : undefined}
                    color={color}
                  />
                  <ShootingBar label="Lancers francs" pct={player.freeThrowPct} color={color} />
                </div>
              </>
            ) : (
              <p className="text-sm text-muted-foreground">Pas encore de statistiques pour cette saison.</p>
            )}

            <RecentForm playerId={player.id} color={color} />
          </div>
        </DialogContent>
      )}
    </Dialog>
  );
}
