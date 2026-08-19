"use client";

import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import { fetchPlayers } from "@/lib/api/players";

export default function PlayersPage() {
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");

  // Debounce classique : on ne relance la recherche que 300ms après la
  // dernière frappe. La recherche tape sur ta propre base (pas balldontlie),
  // donc pas un souci de quota — juste plus propre côté UX.
  useEffect(() => {
    const timeoutId = window.setTimeout(() => setDebouncedSearch(search), 300);
    return () => window.clearTimeout(timeoutId);
  }, [search]);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["players", "search", debouncedSearch],
    queryFn: () => fetchPlayers(debouncedSearch ? { search: debouncedSearch } : undefined),
    staleTime: 60 * 60 * 1000,
  });

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-bold">Joueurs</h1>
        <p className="mt-1 text-muted-foreground">Explore les effectifs des 30 franchises NBA.</p>
      </div>

      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Chercher un joueur (nom ou prénom)..."
          className="pl-9"
        />
      </div>

      {isLoading && <BasketballLoader label="Chargement des joueurs..." />}

      {isError && <p className="text-destructive">Impossible de charger les joueurs.</p>}

      {!isLoading && !isError && data && data.length === 0 && (
        <p className="text-muted-foreground">
          Aucun joueur trouvé. Si la liste est vide sans recherche, l&apos;effectif n&apos;a
          peut-être pas encore été synchronisé côté serveur.
        </p>
      )}

      {!isLoading && !isError && data && data.length > 0 && (
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
