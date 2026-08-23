import Link from "next/link";
import { Trophy } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { PlayerAvatar } from "@/components/player-avatar";
import { LeaderboardEntry } from "@/lib/types";
import { cn } from "@/lib/utils";

const RANK_COLORS: Record<number, string> = {
  1: "text-primary",
  2: "text-muted-foreground",
  3: "text-muted-foreground",
};

function Row({ entry, highlight }: { entry: LeaderboardEntry; highlight: boolean }) {
  return (
    <div
      className={cn(
        "flex items-center justify-between gap-3 rounded-md px-2 py-1.5 text-sm",
        highlight && "border border-primary/30 bg-primary/5"
      )}
    >
      <div className="flex items-center gap-2">
        {entry.rank <= 3 ? (
          <Trophy className={cn("h-3.5 w-3.5", RANK_COLORS[entry.rank])} />
        ) : (
          <span className="w-3.5" />
        )}
        <span className={cn("font-mono text-xs font-bold", RANK_COLORS[entry.rank])}>
          #{entry.rank}
        </span>
        <Link
          href={`/u/${encodeURIComponent(entry.username)}`}
          className={cn(
            "flex min-w-0 items-center gap-1.5 truncate hover:underline",
            highlight && "font-semibold text-primary"
          )}
        >
          <PlayerAvatar
            number={entry.avatarNumber}
            position={entry.avatarPosition}
            colorway={entry.avatarColorway}
            icon={entry.avatarIcon}
            size="xs"
          />
          <span className="truncate">{entry.username}</span>
        </Link>
      </div>
      <span className="font-mono text-xs font-bold">{entry.points.toLocaleString("fr-FR")}</span>
    </div>
  );
}

export function LeaderboardPreview({
  entries,
  currentUsername,
  isLoading,
}: {
  entries: LeaderboardEntry[];
  currentUsername?: string;
  isLoading: boolean;
}) {
  const top = entries.slice(0, 5);
  const own = entries.find((e) => e.username === currentUsername);
  const ownIsInTop = own && top.some((e) => e.username === own.username);

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle className="font-heading text-base">Classement</CardTitle>
        <Link href="/leaderboard" className="text-xs font-medium text-primary hover:underline">
          Tout voir
        </Link>
      </CardHeader>
      <CardContent className="flex flex-col gap-1">
        {isLoading &&
          Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-7 w-full" />)}

        {!isLoading && top.length === 0 && (
          <p className="py-4 text-center text-sm text-muted-foreground">
            Le classement n&apos;est pas encore ouvert.
          </p>
        )}

        {!isLoading &&
          top.map((entry) => (
            <Row key={entry.rank} entry={entry} highlight={entry.username === currentUsername} />
          ))}

        {!isLoading && own && !ownIsInTop && (
          <>
            <div className="my-1 text-center text-xs text-muted-foreground">···</div>
            <Row entry={own} highlight />
          </>
        )}
      </CardContent>
    </Card>
  );
}
