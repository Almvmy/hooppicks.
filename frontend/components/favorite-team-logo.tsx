"use client";

import { useQuery } from "@tanstack/react-query";
import { TeamLogo } from "@/components/team-logo";
import { fetchTeamRankings } from "@/lib/api/teams";

/**
 * Logo de l'équipe favorite. Elle est stockée sous son nom complet
 * ("Boston Celtics", cf. nba-teams.ts) alors que les équipes en base portent
 * le nom court ("Celtics") : on les rapproche par la fin du nom.
 */
export function FavoriteTeamLogo({ teamName, size = 28 }: { teamName: string; size?: number }) {
  const { data: teams } = useQuery({
    queryKey: ["teams", "rankings"],
    queryFn: fetchTeamRankings,
    staleTime: 60 * 60 * 1000,
  });
  const team = teams?.find((t) => teamName.endsWith(t.name));
  if (!team) return null;
  return <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={size} />;
}
