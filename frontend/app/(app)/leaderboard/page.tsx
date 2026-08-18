"use client";

import { useQuery } from "@tanstack/react-query";
import { Trophy } from "lucide-react";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { PaginationControls, usePagination } from "@/components/ui/pagination-controls";
import { CourtWatermark } from "@/components/court-watermark";
import { fetchLeaderboard } from "@/lib/api/leaderboard";
import { cn } from "@/lib/utils";

const PAGE_SIZE = 20;

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
  const { page, pageCount, pageItems, setPage, totalCount } = usePagination(data, PAGE_SIZE);

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-bold">Classement</h1>
        <p className="mt-1 text-muted-foreground">
          Les meilleurs pronostiqueurs de la saison
          {!isLoading && !isError && totalCount > 0 && ` — ${totalCount} joueurs`}.
        </p>
      </div>

      <div className="relative overflow-hidden rounded-lg border border-border bg-card">
        <CourtWatermark className="pointer-events-none absolute -right-6 -top-10 h-[220px] w-[340px] opacity-[0.05]" />
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
            {isLoading && (
              <TableRow>
                <TableCell colSpan={5}>
                  <BasketballLoader label="Chargement du classement..." />
                </TableCell>
              </TableRow>
            )}

            {isError && (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-destructive">
                  Impossible de charger le classement.
                </TableCell>
              </TableRow>
            )}

            {!isLoading &&
              !isError &&
              pageItems.map((entry) => (
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

      {!isLoading && !isError && totalCount > 0 && (
        <PaginationControls page={page} pageCount={pageCount} onPageChange={setPage} />
      )}
    </div>
  );
}
