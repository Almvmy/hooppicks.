"use client";

import { useQuery } from "@tanstack/react-query";
import { PaginationControls, usePagination } from "@/components/ui/pagination-controls";
import { CourtWatermark } from "@/components/court-watermark";
import { NetPattern } from "@/components/net-pattern";
import { LeaderboardTable } from "@/components/leaderboard-table";
import { fetchLeaderboard } from "@/lib/api/leaderboard";

const PAGE_SIZE = 20;

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
        <NetPattern className="pointer-events-none absolute -bottom-10 -left-8 h-[200px] w-[260px] opacity-[0.05]" />
        <LeaderboardTable entries={pageItems} isLoading={isLoading} isError={isError} />
      </div>

      {!isLoading && !isError && totalCount > 0 && (
        <PaginationControls page={page} pageCount={pageCount} onPageChange={setPage} />
      )}
    </div>
  );
}
