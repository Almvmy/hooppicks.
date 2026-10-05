"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { TeamLogo } from "@/components/team-logo";
import { fetchMatches } from "@/lib/api/matches";
import { BetSelection, Match } from "@/lib/types";
import { cn } from "@/lib/utils";

/**
 * Matchs indexés par id, depuis le même cache ["matches"] que la page Matchs :
 * un pari ne stocke que l'id de son match, c'est ce qui permet de retrouver
 * les équipes (et leurs logos) sans nouvel appel ni changement backend.
 */
export function useMatchesById(): Map<string, Match> {
  const { data } = useQuery({ queryKey: ["matches"], queryFn: fetchMatches, staleTime: 60 * 1000 });
  return useMemo(() => new Map((data ?? []).map((m) => [m.id, m])), [data]);
}

/**
 * Logo de l'équipe choisie dans une sélection (vainqueur, écart) ; pour un
 * pari sur le total, qui ne porte sur aucune équipe, les deux logos se
 * chevauchent. Rien tant que le match n'est pas connu.
 */
export function SelectionTeamLogo({
  selection,
  matchesById,
  size = 22,
  className,
}: {
  selection: Pick<BetSelection, "matchId" | "outcome">;
  matchesById: Map<string, Match>;
  size?: number;
  className?: string;
}) {
  const match = matchesById.get(selection.matchId);
  if (!match) return null;

  if (selection.outcome === "home" || selection.outcome === "away") {
    const team = selection.outcome === "home" ? match.homeTeam : match.awayTeam;
    return (
      <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={size} className={className} />
    );
  }

  return (
    <span className={cn("flex shrink-0 items-center", className)}>
      <TeamLogo abbreviation={match.awayTeam.abbreviation} logoUrl={match.awayTeam.logoUrl} size={size} />
      <TeamLogo
        abbreviation={match.homeTeam.abbreviation}
        logoUrl={match.homeTeam.logoUrl}
        size={size}
        className="-ml-1.5"
      />
    </span>
  );
}
