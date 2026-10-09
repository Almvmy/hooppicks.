"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, ChevronRight, UserX, Users } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchTeamRoster } from "@/lib/api/teams";
import { PlayerCardDialog } from "@/components/player-card-dialog";
import { TeamLogo } from "@/components/team-logo";
import { RosterPlayer, Team } from "@/lib/types";

// Jour de match : la rotation probable (ESPN ne publie pas la feuille de
// match avant le coup d'envoi) = les plus utilisés en minutes, hors forfaits ;
// puis les forfaits qui comptent vraiment dans la rotation.
const ROTATION_SIZE = 12;
const KEY_ABSENCE_MINUTES = 15;

const isOut = (p: RosterPlayer) => p.injuryStatus?.toLowerCase() === "out";
const byMinutes = (a: RosterPlayer, b: RosterPlayer) => (b.minutesPerGame ?? -1) - (a.minutesPerGame ?? -1);

function PlayerRow({ player, onSelect }: { player: RosterPlayer; onSelect: (p: RosterPlayer) => void }) {
  return (
    <button
      type="button"
      onClick={() => onSelect(player)}
      className="flex w-full items-center gap-2 rounded-md px-1.5 py-1 text-left transition-colors hover:bg-tint/5"
    >
      {player.headshotUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={player.headshotUrl}
          alt=""
          className="h-7 w-7 shrink-0 rounded-full bg-tint/10 object-cover"
        />
      ) : (
        <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-tint/10 font-mono text-[10px] text-muted-foreground">
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
                className="ml-1.5 inline-block h-3 w-3 text-amber-500 light:text-amber-800"
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
  );
}

/**
 * Effectif actuel de l'équipe, sourcé depuis ESPN (voir EspnRosterService
 * côté backend). Avant, on ne pouvait que "chercher" un joueur dans
 * balldontlie : son free tier ne distingue pas actif/retraité, un listing
 * brut aurait mélangé l'effectif du moment avec des décennies d'historique.
 *
 * `matchday` : page d'un match, seulement la rotation probable et les grands
 * absents, avec un lien vers l'effectif complet (page de l'équipe).
 */
export function TeamRoster({
  team,
  matchday = false,
}: {
  team: Pick<Team, "id" | "name" | "abbreviation" | "logoUrl">;
  matchday?: boolean;
}) {
  const teamId = team.id;
  const [selectedPlayer, setSelectedPlayer] = useState<RosterPlayer | null>(null);
  const { data, isLoading, isError } = useQuery({
    queryKey: ["team-roster", teamId],
    queryFn: () => fetchTeamRoster(teamId),
    staleTime: 60 * 60 * 1000,
  });
  const rotation = (data ?? []).filter((p) => !isOut(p)).sort(byMinutes).slice(0, ROTATION_SIZE);
  const absents = (data ?? []).filter((p) => isOut(p) && (p.minutesPerGame ?? 0) >= KEY_ABSENCE_MINUTES).sort(byMinutes);

  return (
    <div className="flex flex-col gap-2">
      <p className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
        <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={22} />
        <Users className="h-3.5 w-3.5" />
        {matchday ? `Rotation probable : ${team.name}` : `Effectif : ${team.name}`}
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

      {!isLoading && !isError && data && data.length > 0 && matchday && (
        <>
          <ul className="flex flex-col gap-1 text-sm">
            {rotation.map((player) => (
              <li key={player.id}>
                <PlayerRow player={player} onSelect={setSelectedPlayer} />
              </li>
            ))}
          </ul>
          {absents.length > 0 && (
            <div className="mt-1 flex flex-col gap-1">
              <p className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-destructive">
                <UserX className="h-3.5 w-3.5" />
                Grands absents
              </p>
              <ul className="flex flex-col gap-1 text-sm opacity-80">
                {absents.map((player) => (
                  <li key={player.id}>
                    <PlayerRow player={player} onSelect={setSelectedPlayer} />
                  </li>
                ))}
              </ul>
            </div>
          )}
          <Link
            href={`/teams/${team.id}`}
            className="flex items-center gap-1 self-start text-xs font-medium text-primary hover:underline"
          >
            Tout l&apos;effectif
            <ChevronRight className="h-3.5 w-3.5" />
          </Link>
        </>
      )}

      {!isLoading && !isError && data && data.length > 0 && !matchday && (
        <ul className="flex flex-col gap-1 text-sm">
          {data.map((player) => (
            <li key={player.id}>
              <PlayerRow player={player} onSelect={setSelectedPlayer} />
            </li>
          ))}
        </ul>
      )}

      <PlayerCardDialog player={selectedPlayer} onOpenChange={(open) => !open && setSelectedPlayer(null)} />
    </div>
  );
}
