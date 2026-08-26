"use client";

import { useQuery } from "@tanstack/react-query";
import { Star } from "lucide-react";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchMatchBoxScore } from "@/lib/api/matches";
import { PlayerBoxScore } from "@/lib/types";
import { cn } from "@/lib/utils";

function TeamBoxScore({ teamName, players }: { teamName: string; players: PlayerBoxScore[] }) {
  // Déjà trié par points côté serveur, mais on le refait ici : le tableau
  // peut contenir les deux équipes mélangées avant filtrage par team.
  const sorted = [...players].sort((a, b) => b.points - a.points);
  const topScorerName = sorted[0]?.playerName;

  return (
    <div className="overflow-x-auto">
      <p className="mb-2 font-heading text-sm font-bold">{teamName}</p>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Joueur</TableHead>
            <TableHead className="text-right">MIN</TableHead>
            <TableHead className="text-right">PTS</TableHead>
            <TableHead className="text-right">REB</TableHead>
            <TableHead className="text-right">PD</TableHead>
            <TableHead className="text-right">TIRS</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {sorted.map((p) => (
            <TableRow key={p.playerName}>
              <TableCell className="font-medium">
                <span className="flex items-center gap-1.5">
                  {p.playerName === topScorerName && (
                    <Star className="h-3.5 w-3.5 shrink-0 fill-primary text-primary" aria-label="Meilleur marqueur" />
                  )}
                  <span className={cn(!p.starter && "text-muted-foreground")}>{p.playerName}</span>
                </span>
              </TableCell>
              <TableCell className="text-right font-mono text-xs text-muted-foreground">{p.minutes}</TableCell>
              <TableCell className="text-right font-mono font-bold">{p.points}</TableCell>
              <TableCell className="text-right font-mono text-xs">{p.rebounds}</TableCell>
              <TableCell className="text-right font-mono text-xs">{p.assists}</TableCell>
              <TableCell className="text-right font-mono text-xs text-muted-foreground">{p.fieldGoals}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

export function MatchBoxScore({
  matchId,
  homeTeamName,
  homeTeamAbbr,
  awayTeamName,
  awayTeamAbbr,
}: {
  matchId: string;
  homeTeamName: string;
  homeTeamAbbr: string;
  awayTeamName: string;
  awayTeamAbbr: string;
}) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["match-boxscore", matchId],
    queryFn: () => fetchMatchBoxScore(matchId),
  });

  if (isLoading) {
    return (
      <div className="grid gap-6 sm:grid-cols-2">
        <Skeleton className="h-64 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (isError) {
    return <p className="text-sm text-destructive">Impossible de charger la feuille de match.</p>;
  }

  if (!data || data.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        Feuille de match pas encore disponible pour ce match.
      </p>
    );
  }

  const homePlayers = data.filter((p) => p.teamAbbreviation === homeTeamAbbr);
  const awayPlayers = data.filter((p) => p.teamAbbreviation === awayTeamAbbr);

  return (
    <div className="grid gap-6 sm:grid-cols-2">
      <TeamBoxScore teamName={awayTeamName} players={awayPlayers} />
      <TeamBoxScore teamName={homeTeamName} players={homePlayers} />
    </div>
  );
}
