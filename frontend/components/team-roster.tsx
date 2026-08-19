"use client";

import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search, Users } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchPlayers } from "@/lib/api/players";

const MIN_SEARCH_LENGTH = 2;

/**
 * Plutôt qu'une liste brute de tout l'historique d'une équipe (voir
 * l'ancienne version : balldontlie en free tier ne distingue pas actif vs
 * retraité, et tronquait silencieusement au-delà de 100 entrées), on laisse
 * chercher un joueur précis de cette équipe par nom.
 */
export function TeamRoster({ teamId, teamName }: { teamId: string; teamName: string }) {
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");

  useEffect(() => {
    const timeoutId = window.setTimeout(() => setDebouncedSearch(search), 300);
    return () => window.clearTimeout(timeoutId);
  }, [search]);

  const isSearchValid = debouncedSearch.trim().length >= MIN_SEARCH_LENGTH;

  const { data, isLoading } = useQuery({
    queryKey: ["players", "team", teamId, debouncedSearch],
    queryFn: () => fetchPlayers({ teamId, search: debouncedSearch }),
    enabled: isSearchValid,
    staleTime: 60 * 60 * 1000,
  });

  return (
    <div className="flex flex-col gap-2">
      <p className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
        <Users className="h-3.5 w-3.5" />
        Chercher un joueur — {teamName}
      </p>

      <div className="relative">
        <Search className="absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Nom du joueur..."
          className="h-8 pl-8 text-sm"
        />
      </div>

      {isSearchValid && isLoading && (
        <div className="flex flex-col gap-1.5">
          <Skeleton className="h-5 w-full" />
        </div>
      )}

      {isSearchValid && !isLoading && data && data.length === 0 && (
        <p className="text-sm text-muted-foreground">Aucun joueur trouvé.</p>
      )}

      {isSearchValid && !isLoading && data && data.length > 0 && (
        <ul className="flex flex-col gap-1 text-sm">
          {data.map((player) => (
            <li key={player.id} className="flex items-baseline justify-between gap-2">
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