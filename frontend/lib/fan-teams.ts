import { favoriteTeamAbbreviation } from "@/lib/use-teams";
import { LeaderboardEntry, TeamRank } from "@/lib/types";

export interface FanTeam {
  abbreviation: string;
  /** Supporters classés, dans l'ordre du classement général. */
  fans: LeaderboardEntry[];
  averagePoints: number;
  averageWinRate: number;
}

/**
 * Classement des communautés de supporters : moyenne de points par fan (et
 * pas total), pour qu'une équipe à 2 supporters brillants ne soit pas
 * écrasée par une équipe à 30 supporters moyens.
 */
export function rankFanTeams(entries: LeaderboardEntry[], teams: Map<string, TeamRank>): FanTeam[] {
  const byTeam = new Map<string, LeaderboardEntry[]>();
  for (const e of entries) {
    const abbr = favoriteTeamAbbreviation(e.favoriteTeam ?? undefined, teams);
    if (!abbr) continue;
    byTeam.set(abbr, [...(byTeam.get(abbr) ?? []), e]);
  }
  return [...byTeam.entries()]
    .map(([abbreviation, fans]) => ({
      abbreviation,
      fans,
      averagePoints: Math.round(fans.reduce((sum, f) => sum + f.points, 0) / fans.length),
      averageWinRate: Math.round(fans.reduce((sum, f) => sum + f.winRate, 0) / fans.length),
    }))
    .sort((a, b) => b.averagePoints - a.averagePoints || b.fans.length - a.fans.length);
}
