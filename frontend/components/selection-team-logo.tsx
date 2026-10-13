"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { TeamLogo } from "@/components/team-logo";
import { DEFAULT_PAST_DAYS, fetchMatches, fetchMatchesByIds } from "@/lib/api/matches";
import { BetSelection, Match } from "@/lib/types";
import { cn } from "@/lib/utils";

/** Au-delà, les plus anciens tickets s'affichent sans logo plutôt que d'alourdir l'appel. */
const MAX_EXTRA_IDS = 200;

/**
 * Matchs indexés par id : un pari ne stocke que l'id de son match. Ceux des
 * deux dernières semaines viennent du même cache que la page Matchs ; ceux
 * de tickets plus anciens (matchIds) sont chargés à part, eux seuls.
 */
export function useMatchesById(matchIds: string[] = []): Map<string, Match> {
  const { data } = useQuery({
    queryKey: ["matches", "window", DEFAULT_PAST_DAYS],
    queryFn: () => fetchMatches(),
    staleTime: 60 * 1000,
  });
  const missing = useMemo(() => {
    if (!data) return [];
    const known = new Set(data.map((m) => m.id));
    return [...new Set(matchIds)].filter((id) => !known.has(id)).slice(0, MAX_EXTRA_IDS);
  }, [data, matchIds]);
  const { data: extra } = useQuery({
    queryKey: ["matches", "ids", missing.join(",")],
    queryFn: () => fetchMatchesByIds(missing),
    enabled: missing.length > 0,
    staleTime: 5 * 60 * 1000,
  });
  return useMemo(() => new Map([...(data ?? []), ...(extra ?? [])].map((m) => [m.id, m])), [data, extra]);
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
