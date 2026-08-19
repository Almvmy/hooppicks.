import { useQuery } from "@tanstack/react-query";
import { Users } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchPlayers } from "@/lib/api/players";

/**
 * Effectif d'une équipe donnée. Si la synchro des joueurs n'a jamais été
 * lancée côté admin (POST /admin/nba/sync-players), la liste sera vide —
 * on l'indique clairement plutôt que de laisser un vide silencieux.
 */
export function TeamRoster({ teamId, teamName }: { teamId: string; teamName: string }) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["players", "team", teamId],
    queryFn: () => fetchPlayers({ teamId }),
    staleTime: 60 * 60 * 1000, // 1h : les effectifs bougent très peu
  });

  return (
    <div className="flex flex-col gap-2">
      <p className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
        <Users className="h-3.5 w-3.5" />
        Effectif {teamName}
      </p>

      {isLoading && (
        <div className="flex flex-col gap-1.5">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-5 w-full" />
          ))}
        </div>
      )}

      {isError && <p className="text-sm text-muted-foreground">Effectif indisponible.</p>}

      {!isLoading && !isError && (!data || data.length === 0) && (
        <p className="text-sm text-muted-foreground">Effectif pas encore synchronisé.</p>
      )}

      {!isLoading && !isError && data && data.length > 0 && (
        <ul className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm sm:grid-cols-3">
          {data.map((player) => (
            <li key={player.id} className="flex items-baseline justify-between gap-1 truncate">
              <span className="truncate">
                {player.firstName} {player.lastName}
              </span>
              {player.position && (
                <span className="shrink-0 font-mono text-xs text-muted-foreground">
                  {player.position}
                </span>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
