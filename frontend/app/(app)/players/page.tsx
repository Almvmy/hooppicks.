"use client";

import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import { fetchPlayers } from "@/lib/api/players";

const MIN_SEARCH_LENGTH = 2;

export default function PlayersPage() {
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");

  useEffect(() => {
    const timeoutId = window.setTimeout(() => setDebouncedSearch(search), 300);
    return () => window.clearTimeout(timeoutId);
  }, [search]);

  const isSearchValid = debouncedSearch.trim().length >= MIN_SEARCH_LENGTH;

  const { data, isLoading, isError } = useQuery({
    queryKey: ["players", "search", debouncedSearch],
    queryFn: () => fetchPlayers({ search: debouncedSearch }),
    enabled: isSearchValid,
    staleTime: 60 * 60 * 1000,
  });

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-bold">Joueurs</h1>
        <p className="mt-1 text-muted-foreground">
          Cherche un joueur, actuel ou historique, parmi toute la base NBA.
        </p>
      </div>

      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Chercher un joueur (nom ou prénom)..."
          className="pl-9"
          autoFocus
        />
      </div>

      {!isSearchValid && (
        <p className="text-sm text-muted-foreground">
          Tape au moins {MIN_SEARCH_LENGTH} caractères pour lancer la recherche.
        </p>
      )}

      {isSearchValid && isLoading && <BasketballLoader label="Recherche en cours..." />}

      {isSearchValid && isError && <p className="text-destructive">Impossible de charger les joueurs.</p>}

      {isSearchValid && !isLoading && !isError && data && data.length === 0 && (
        <p className="text-muted-foreground">
          Aucun joueur trouvé pour &quot;{debouncedSearch}&quot;.
        </p>
      )}

      {isSearchValid && !isLoading && !isError && data && data.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {data.map((player) => (
            <Card key={player.id} className="border-border bg-card">
              <CardContent className="flex items-center justify-between gap-3 pt-6">
                <div className="min-w-0">
                  <p className="truncate font-medium">
                    {player.firstName} {player.lastName}
                  </p>
                  <p className="truncate text-xs text-muted-foreground">
                    {player.team?.name ?? "Équipe inconnue"}
                  </p>
                </div>
                <div className="flex shrink-0 flex-col items-end gap-0.5 text-right">
                  {player.position && (
                    <span className="font-mono text-xs font-bold text-primary">{player.position}</span>
                  )}
                  {player.height && (
                    <span className="font-mono text-[11px] text-muted-foreground">{player.height} ft</span>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}