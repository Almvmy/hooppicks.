"use client";

import { useQuery } from "@tanstack/react-query";
import { Trophy } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { fetchLeaderboard } from "@/lib/api/leaderboard";
import { cn } from "@/lib/utils";

const RANK_COLORS: Record<number, string> = {
  1: "text-primary",
  2: "text-muted-foreground",
  3: "text-muted-foreground",
};

export default function LeaderboardPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["leaderboard"],
    queryFn: fetchLeaderboard,
    staleTime: 2 * 60 * 1000, // 2 minutes  
  });

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-bold">Classement</h1>
        <p className="mt-1 text-muted-foreground">
          Les meilleurs pronostiqueurs de la saison.
        </p>
      </div>

      <div className="rounded-lg border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-16">Rang</TableHead>
              <TableHead>Joueur</TableHead>
              <TableHead className="text-right">Taux de réussite</TableHead>
              <TableHead className="text-right">Paris joués</TableHead>
              <TableHead className="text-right">Points</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading &&
              Array.from({ length: 5 }).map((_, i) => (
                <TableRow key={i}>
                  <TableCell colSpan={5}>
                    <Skeleton className="h-6 w-full" />
                  </TableCell>
                </TableRow>
              ))}

            {isError && (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-destructive">
                  Impossible de charger le classement.
                </TableCell>
              </TableRow>
            )}

            {!isLoading &&
              !isError &&
              data?.map((entry) => (
                <TableRow key={entry.rank}>
                  <TableCell>
                    <div className="flex items-center gap-1.5">
                      {entry.rank <= 3 && (
                        <Trophy
                          className={cn("h-4 w-4", RANK_COLORS[entry.rank])}
                        />
                      )}
                      <span
                        className={cn(
                          "font-mono font-bold",
                          RANK_COLORS[entry.rank]
                        )}
                      >
                        #{entry.rank}
                      </span>
                    </div>
                  </TableCell>
                  <TableCell className="font-medium">{entry.username}</TableCell>
                  <TableCell className="text-right text-muted-foreground">
                    {entry.winRate}%
                  </TableCell>
                  <TableCell className="text-right text-muted-foreground">
                    {entry.totalBets}
                  </TableCell>
                  <TableCell className="text-right font-mono font-bold">
                    {entry.points.toLocaleString("fr-FR")}
                  </TableCell>
                </TableRow>
              ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}