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
import { LeaderboardEntry } from "@/lib/types";
import { cn } from "@/lib/utils";

const RANK_COLORS: Record<number, string> = {
  1: "text-primary",
  2: "text-muted-foreground",
  3: "text-muted-foreground",
};

export function LeaderboardTable({
  entries,
  isLoading,
  isError,
  emptyMessage = "Aucun joueur pour l'instant.",
  currentUsername,
}: {
  entries: LeaderboardEntry[] | undefined;
  isLoading: boolean;
  isError: boolean;
  emptyMessage?: string;
  currentUsername?: string;
}) {
  return (
    <Table>
      <TableHeader>
        <TableRow className="border-b-0 shadow-[inset_0_-1px_0_rgba(255,255,255,0.08)] hover:bg-transparent">
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

        {!isLoading && !isError && entries?.length === 0 && (
          <TableRow>
            <TableCell colSpan={5} className="text-center text-muted-foreground">
              {emptyMessage}
            </TableCell>
          </TableRow>
        )}

        {!isLoading &&
          !isError &&
          entries?.map((entry) => (
            <TableRow
              key={entry.rank}
              className={cn(entry.username === currentUsername && "glass-accent border-b-0")}
            >
              <TableCell>
                <div className="flex items-center gap-1.5">
                  {entry.rank <= 3 && (
                    <Trophy className={cn("h-4 w-4", RANK_COLORS[entry.rank])} />
                  )}
                  <span className={cn("font-mono font-bold", RANK_COLORS[entry.rank])}>
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
  );
}
