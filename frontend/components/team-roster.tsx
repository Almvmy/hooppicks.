"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, Users } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchTeamRoster } from "@/lib/api/teams";
import { PlayerCardDialog } from "@/components/player-card-dialog";
import { RosterPlayer } from "@/lib/types";

/**
 * Effectif actuel de l'équipe, sourcé depuis ESPN (voir EspnRosterService
 * côté backend). Avant, on ne pouvait que "chercher" un joueur dans
 * balldontlie : son free tier ne distingue pas actif/retraité, un listing
 * brut aurait mélangé l'effectif du moment avec des décennies d'historique.
 */
export function TeamRoster({ teamId, teamName }: { teamId: string; teamName: string }) {
  const [selectedPlayer, setSelectedPlayer] = useState<RosterPlayer | null>(null);
  const { data, isLoading, isError } = useQuery({
    queryKey: ["team-roster", teamId],
    queryFn: () => fetchTeamRoster(teamId),
    staleTime: 60 * 60 * 1000,
  });

  return (
    <div className="flex flex-col gap-2">
      <p className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
        <Users className="h-3.5 w-3.5" />
        Effectif — {teamName}
      </p>

      {isLoading && (
        <div className="flex flex-col gap-1.5">
          <Skeleton className="h-5 w-full" />
          <Skeleton className="h-5 w-full" />
          <Skeleton className="h-5 w-3/4" />
        </div>
      )}

      {isError && <p className="text-sm text-destructive">Impossible de charger l&apos;effectif.</p>}

      {!isLoading && !isError && data && data.length === 0 && (
        <p className="text-sm text-muted-foreground">Effectif pas encore disponible pour cette équipe.</p>
      )}

      {!isLoading && !isError && data && data.length > 0 && (
        <ul className="flex flex-col gap-1 text-sm">
          {data.map((player) => (
            <li key={player.id}>
              <button
                type="button"
                onClick={() => setSelectedPlayer(player)}
                className="flex w-full items-center gap-2 rounded-md px-1.5 py-1 text-left transition-colors hover:bg-white/5"
              >
                {player.headshotUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img
                    src={player.headshotUrl}
                    alt=""
                    className="h-7 w-7 shrink-0 rounded-full bg-white/10 object-cover"
                  />
                ) : (
                  <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-white/10 font-mono text-[10px] text-muted-foreground">
                    {player.jersey ?? ""}
                  </span>
                )}
                <div className="min-w-0 flex-1">
                  <div className="flex items-baseline justify-between gap-2">
                    <span className="truncate">
                      {player.jersey && (
                        <span className="mr-1.5 font-mono text-xs text-muted-foreground">#{player.jersey}</span>
                      )}
                      {player.firstName} {player.lastName}
                      {player.injuryStatus && (
                        <AlertTriangle
                          className="ml-1.5 inline-block h-3 w-3 text-amber-500"
                          aria-label={player.injuryStatus}
                        />
                      )}
                    </span>
                    {player.position && (
                      <span className="shrink-0 font-mono text-xs text-muted-foreground">{player.position}</span>
                    )}
                  </div>
                  {player.pointsPerGame !== null && (
                    <p className="truncate font-mono text-[11px] text-muted-foreground">
                      {player.pointsPerGame.toFixed(1)} pts · {player.reboundsPerGame?.toFixed(1)} reb ·{" "}
                      {player.assistsPerGame?.toFixed(1)} pd
                      {player.statsSeasonLabel && ` (${player.statsSeasonLabel})`}
                    </p>
                  )}
                </div>
              </button>
            </li>
          ))}
        </ul>
      )}

      <PlayerCardDialog player={selectedPlayer} onOpenChange={(open) => !open && setSelectedPlayer(null)} />
    </div>
  );
}
