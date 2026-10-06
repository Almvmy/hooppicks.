"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchTeamRankings } from "@/lib/api/teams";
import { fetchProfile } from "@/lib/api/auth";
import { TeamRank } from "@/lib/types";

/** Les 30 équipes indexées par sigle (même cache que la page Équipes). */
export function useTeamsByAbbreviation(): Map<string, TeamRank> {
  const { data } = useQuery({
    queryKey: ["teams", "rankings"],
    queryFn: fetchTeamRankings,
    staleTime: 60 * 60 * 1000,
  });
  return useMemo(() => new Map((data ?? []).map((t) => [t.abbreviation, t])), [data]);
}

/**
 * Sigle de l'équipe favorite, stockée sous son nom complet ("Boston Celtics",
 * cf. nba-teams.ts) alors que la base porte le nom court ("Celtics").
 */
export function favoriteTeamAbbreviation(favoriteTeam: string | undefined, teams: Map<string, TeamRank>): string | null {
  if (!favoriteTeam) return null;
  for (const team of teams.values()) {
    // Même règle que le backend (FavoriteTeamService.matchesTeam) : le nom
    // court précédé d'une espace, pour que "Nets" ne réponde pas à "Hornets".
    if (favoriteTeam === team.name || favoriteTeam.endsWith(` ${team.name}`)) return team.abbreviation;
  }
  return null;
}

/** L'équipe favorite du joueur connecté (undefined si aucune, ou pas encore chargée). */
export function useFavoriteTeam(): { team: TeamRank | undefined; isLoading: boolean } {
  const { data: profile, isLoading } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const teams = useTeamsByAbbreviation();
  const abbreviation = favoriteTeamAbbreviation(profile?.favoriteTeam, teams);
  return { team: abbreviation ? teams.get(abbreviation) : undefined, isLoading: isLoading || (!!profile?.favoriteTeam && teams.size === 0) };
}
