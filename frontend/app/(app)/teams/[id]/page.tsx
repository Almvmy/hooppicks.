"use client";

import { use, useMemo } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Trophy } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import { TeamLogo } from "@/components/team-logo";
import { TeamRoster } from "@/components/team-roster";
import { MatchCard } from "@/components/match-card";
import { fetchTeamRankings } from "@/lib/api/teams";
import { fetchMatches } from "@/lib/api/matches";

export default function TeamDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);

  const { data: teams, isLoading: isLoadingTeam, isError: isTeamError } = useQuery({
    queryKey: ["teams", "rankings"],
    queryFn: fetchTeamRankings,
    staleTime: 5 * 60 * 1000,
  });

  const { data: matches, isLoading: isLoadingMatches, isError: isMatchesError } = useQuery({
    queryKey: ["matches"],
    queryFn: fetchMatches,
    staleTime: 60 * 1000,
  });

  const team = teams?.find((t) => t.id === id);

  const { recent, upcoming } = useMemo(() => {
    const teamMatches = (matches ?? []).filter((m) => m.homeTeam.id === id || m.awayTeam.id === id);
    return {
      recent: teamMatches
        .filter((m) => m.status === "finished")
        .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
        .slice(0, 5),
      upcoming: teamMatches
        .filter((m) => m.status !== "finished")
        .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
        .slice(0, 5),
    };
  }, [matches, id]);

  return (
    <div className="flex flex-col gap-6">
      <Link
        href="/players"
        className="flex w-fit items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" />
        Retour aux équipes
      </Link>

      {isLoadingTeam && <Skeleton className="h-40 w-full rounded-lg" />}
      {isTeamError && <p className="text-destructive">Impossible de charger cette équipe.</p>}
      {!isLoadingTeam && !isTeamError && !team && (
        <p className="text-muted-foreground">Cette équipe n&apos;existe pas.</p>
      )}

      {team && (
        <Card>
          <CardContent className="flex flex-col items-center gap-3 pt-6 text-center">
            <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={64} />
            <div>
              <h1 className="font-heading text-2xl font-bold">{team.name}</h1>
              <p className="text-sm text-muted-foreground">
                {team.conference} · {team.division}
              </p>
            </div>
            <div className="flex flex-wrap items-center justify-center gap-3 text-sm">
              <span className="rounded-full bg-primary/10 px-3 py-1 font-mono text-xs font-bold text-primary">
                Force Elo #{team.rank}
              </span>
              {team.wins !== null && team.losses !== null && (
                <span className="flex items-center gap-1.5 font-mono text-xs text-muted-foreground">
                  <Trophy className="h-3.5 w-3.5 text-primary" />
                  {team.wins}-{team.losses}
                  {team.conferenceSeed !== null && ` · seed #${team.conferenceSeed}`}
                  {team.streak && ` · série ${team.streak}`}
                  {team.gamesBehind && team.gamesBehind !== "-" && ` · ${team.gamesBehind} GB`}
                </span>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {team && (
        <Card>
          <CardContent className="pt-6">
            <TeamRoster teamId={team.id} teamName={team.name} />
          </CardContent>
        </Card>
      )}

      {isLoadingMatches && <BasketballLoader label="Chargement des matchs..." />}
      {isMatchesError && <p className="text-destructive">Impossible de charger les matchs.</p>}

      {!isLoadingMatches && !isMatchesError && upcoming.length > 0 && (
        <div className="flex flex-col gap-3">
          <h2 className="font-heading text-base font-bold">Prochains matchs</h2>
          <div className="flex flex-col gap-3">
            {upcoming.map((m) => (
              <MatchCard key={m.id} match={m} />
            ))}
          </div>
        </div>
      )}

      {!isLoadingMatches && !isMatchesError && recent.length > 0 && (
        <div className="flex flex-col gap-3">
          <h2 className="font-heading text-base font-bold">Derniers matchs</h2>
          <div className="flex flex-col gap-3">
            {recent.map((m) => (
              <MatchCard key={m.id} match={m} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
