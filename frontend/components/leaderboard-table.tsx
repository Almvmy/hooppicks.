"use client";

import { useLayoutEffect, useRef } from "react";
import Link from "next/link";
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
import { PlayerAvatar } from "@/components/player-avatar";
import { LeaderboardEntry } from "@/lib/types";
import { cn, formatSignedPoints } from "@/lib/utils";
import { prefersReducedMotion } from "@/lib/motion";
import { TeamLogo } from "@/components/team-logo";
import { favoriteTeamAbbreviation, useTeamsByAbbreviation } from "@/lib/use-teams";
import { RankChange, RecentForm, StreakBadge } from "@/components/leaderboard/trends";

const RANK_COLORS: Record<number, string> = {
  1: "text-primary",
  2: "text-muted-foreground",
  3: "text-muted-foreground",
};

/** Taux de réussite + mini-jauge : se compare d'un coup d'œil d'une ligne à l'autre. */
function WinRate({ value }: { value: number }) {
  return (
    <span className="inline-flex items-center justify-end gap-2">
      <span className="hidden h-1.5 w-14 overflow-hidden rounded-full bg-tint/10 sm:inline-block">
        <span className="block h-full rounded-full bg-success" style={{ width: `${Math.min(value, 100)}%` }} />
      </span>
      <span className="w-9 text-right tabular-nums">{value}%</span>
    </span>
  );
}

function FavoriteTeamBadge({
  favoriteTeam,
  teams,
}: {
  favoriteTeam: string | null;
  teams: ReturnType<typeof useTeamsByAbbreviation>;
}) {
  const abbr = favoriteTeamAbbreviation(favoriteTeam ?? undefined, teams);
  if (!abbr) return null;
  const team = teams.get(abbr);
  return (
    <span title={`Supporter des ${favoriteTeam}`} className="inline-flex">
      <TeamLogo abbreviation={abbr} logoUrl={team?.logoUrl} size={18} />
    </span>
  );
}

export function LeaderboardTable({
  entries,
  isLoading,
  isError,
  emptyMessage = "Aucun joueur pour l'instant.",
  currentUsername,
  showTrends = false,
}: {
  entries: LeaderboardEntry[] | undefined;
  isLoading: boolean;
  isError: boolean;
  emptyMessage?: string;
  currentUsername?: string;
  /** Évolution du rang, forme récente et série (classement général). */
  showTrends?: boolean;
}) {
  const bodyRef = useRef<HTMLTableSectionElement>(null);
  const teams = useTeamsByAbbreviation();
  const previousTops = useRef(new Map<string, number>());

  // FLIP : on mémorise où chaque joueur était, et quand le classement est
  // rechargé avec un ordre différent, sa ligne repart visuellement de son
  // ancienne place et glisse vers la nouvelle. offsetTop (relatif au
  // tableau) et pas getBoundingClientRect : un scroll de la page entre deux
  // rendus déplacerait toutes les lignes et animerait tout le tableau.
  useLayoutEffect(() => {
    const body = bodyRef.current;
    if (!body) return;
    const reduce = prefersReducedMotion();
    const tops = new Map<string, number>();
    body.querySelectorAll<HTMLTableRowElement>("tr[data-flip-key]").forEach((row) => {
      const key = row.dataset.flipKey!;
      const top = row.offsetTop;
      tops.set(key, top);
      const previous = previousTops.current.get(key);
      if (!reduce && previous !== undefined && previous !== top) {
        row.animate(
          [{ transform: `translateY(${previous - top}px)` }, { transform: "translateY(0)" }],
          { duration: 550, easing: "cubic-bezier(0.22, 1, 0.36, 1)" }
        );
      }
    });
    previousTops.current = tops;
  }, [entries]);

  const columns = showTrends ? 6 : 5;

  return (
    <Table>
      <TableHeader>
        <TableRow className="border-b-0 shadow-[inset_0_-1px_0_var(--hairline)] hover:bg-transparent">
          <TableHead className="w-16">Rang</TableHead>
          <TableHead>Joueur</TableHead>
          {showTrends && <TableHead className="hidden md:table-cell">Forme</TableHead>}
          <TableHead className="text-right">Taux de réussite</TableHead>
          <TableHead className="text-right">Paris joués</TableHead>
          <TableHead className="text-right" title="Bénéfice net : gains moins mises">Bénéfice</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody ref={bodyRef} className="stagger-children">
        {isLoading && (
          <TableRow>
            <TableCell colSpan={columns}>
              <BasketballLoader label="Chargement du classement..." />
            </TableCell>
          </TableRow>
        )}

        {isError && (
          <TableRow>
            <TableCell colSpan={columns} className="text-center text-destructive">
              Impossible de charger le classement.
            </TableCell>
          </TableRow>
        )}

        {!isLoading && !isError && entries?.length === 0 && (
          <TableRow>
            <TableCell colSpan={columns} className="text-center text-muted-foreground">
              {emptyMessage}
            </TableCell>
          </TableRow>
        )}

        {!isLoading &&
          !isError &&
          entries?.map((entry) => (
            <TableRow
              // Clé = joueur et pas rang : c'est ce qui permet à React de
              // garder la même ligne quand un joueur change de place.
              key={entry.username}
              data-flip-key={entry.username}
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
                  {showTrends && <RankChange entry={entry} />}
                </div>
              </TableCell>
              <TableCell className="font-medium">
                <Link
                  href={`/u/${encodeURIComponent(entry.username)}`}
                  className="flex items-center gap-2 hover:underline"
                >
                  <PlayerAvatar
                    number={entry.avatarNumber}
                    position={entry.avatarPosition}
                    colorway={entry.avatarColorway}
                    icon={entry.avatarIcon}
                    size="xs"
                  />
                  {entry.username}
                  <FavoriteTeamBadge favoriteTeam={entry.favoriteTeam} teams={teams} />
                  {showTrends && <StreakBadge streak={entry.streak} />}
                </Link>
              </TableCell>
              {showTrends && (
                <TableCell className="hidden md:table-cell">
                  <RecentForm form={entry.recentForm} />
                </TableCell>
              )}
              <TableCell className="text-right text-muted-foreground">
                <WinRate value={entry.winRate} />
              </TableCell>
              <TableCell className="text-right text-muted-foreground">
                {entry.totalBets}
              </TableCell>
              <TableCell className="text-right font-mono font-bold">
                {formatSignedPoints(entry.points, false)}
              </TableCell>
            </TableRow>
          ))}
      </TableBody>
    </Table>
  );
}
