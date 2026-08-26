"use client";

import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search, Shield, Trophy, Users } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import { TeamRoster } from "@/components/team-roster";
import { PlayerCardDialog } from "@/components/player-card-dialog";
import { TeamLogo } from "@/components/team-logo";
import { fetchPlayers } from "@/lib/api/players";
import { fetchTeamRankings } from "@/lib/api/teams";
import { getTeamColor } from "@/lib/team-colors";
import { cn } from "@/lib/utils";
import { RosterPlayer, TeamRank } from "@/lib/types";

const MIN_SEARCH_LENGTH = 2;

/** "6-6" (format brut balldontlie, pieds-pouces) -> 6'6" */
function formatHeight(raw: string | null): string | null {
  if (!raw) return null;
  const [feet, inches] = raw.split("-");
  if (!feet || !inches) return raw;
  return `${feet}'${inches}"`;
}

/**
 * ESPN renvoie déjà "184 lbs" (displayWeight), balldontlie renvoie juste
 * "220" (repli, cf. NbaSyncService.searchAndCachePlayers) — on n'ajoute
 * "lbs" que si ce n'est pas déjà là, sinon "184 lbs lbs".
 */
function formatWeight(raw: string | null): string | null {
  if (!raw) return null;
  return raw.toLowerCase().includes("lbs") ? raw : `${raw} lbs`;
}

function PlayersTab() {
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [positionFilter, setPositionFilter] = useState<string | null>(null);
  const [selectedPlayer, setSelectedPlayer] = useState<RosterPlayer | null>(null);

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

  // Postes réellement présents dans les résultats — balldontlie ne renvoie pas
  // toujours PG/SG/SF/PF/C proprement (parfois "G", "G-F"...), donc on
  // construit le filtre à partir de ce qui existe vraiment plutôt que d'une
  // liste figée.
  const availablePositions = useMemo(() => {
    const set = new Set<string>();
    for (const p of data ?? []) {
      if (p.position) set.add(p.position);
    }
    return [...set].sort();
  }, [data]);

  const filteredPlayers = useMemo(() => {
    if (!data) return [];
    if (!positionFilter) return data;
    return data.filter((p) => p.position === positionFilter);
  }, [data, positionFilter]);

  useEffect(() => {
    setPositionFilter(null);
  }, [debouncedSearch]);

  return (
    <div className="flex flex-col gap-4">
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

      {isSearchValid && !isLoading && !isError && availablePositions.length > 1 && (
        <div className="flex flex-wrap gap-2">
          <button type="button" onClick={() => setPositionFilter(null)}>
            <Badge
              variant="outline"
              className={cn(
                "cursor-pointer border-transparent px-3 py-1.5 transition-colors",
                positionFilter === null
                  ? "glass-accent"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              Tous
            </Badge>
          </button>
          {availablePositions.map((pos) => (
            <button key={pos} type="button" onClick={() => setPositionFilter(pos)}>
              <Badge
                variant="outline"
                className={cn(
                  "cursor-pointer border-transparent px-3 py-1.5 font-mono transition-colors",
                  positionFilter === pos
                    ? "glass-accent"
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                {pos}
              </Badge>
            </button>
          ))}
        </div>
      )}

      {isSearchValid && !isLoading && !isError && filteredPlayers.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {filteredPlayers.map((player) => (
            <Card key={player.id}>
              <button
                type="button"
                onClick={() => setSelectedPlayer(player)}
                className="flex w-full items-center gap-3 px-(--card-spacing) pt-6 text-left transition-colors hover:bg-white/5"
              >
                {player.headshotUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img
                    src={player.headshotUrl}
                    alt={`${player.firstName} ${player.lastName}`}
                    className="h-10 w-10 shrink-0 rounded-full bg-white/10 object-cover"
                  />
                ) : (
                  <span
                    aria-hidden
                    className="h-10 w-10 shrink-0 rounded-full"
                    style={{
                      backgroundColor: player.team ? getTeamColor(player.team.abbreviation) : "#6B7280",
                    }}
                  />
                )}
                <div className="min-w-0 flex-1">
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
                  <span className="font-mono text-[11px] text-muted-foreground">
                    {[formatHeight(player.height), formatWeight(player.weight)]
                      .filter(Boolean)
                      .join(" · ")}
                  </span>
                </div>
              </button>
            </Card>
          ))}
        </div>
      )}

      <PlayerCardDialog player={selectedPlayer} onOpenChange={(open) => !open && setSelectedPlayer(null)} />
    </div>
  );
}

type TeamSortMode = "elo" | "official";

function TeamsTab() {
  const [expandedTeamId, setExpandedTeamId] = useState<string | null>(null);
  const [sortMode, setSortMode] = useState<TeamSortMode>("elo");
  const { data, isLoading, isError } = useQuery({
    queryKey: ["teams", "rankings"],
    queryFn: fetchTeamRankings,
    staleTime: 5 * 60 * 1000,
  });

  // En mode "officiel", le seed n'a de sens que par conférence (deux équipes
  // de conférences différentes peuvent toutes les deux être "seed #1") — donc
  // on groupe Est/Ouest plutôt qu'une seule liste mélangée triée par Elo.
  const sortedData = useMemo(() => {
    if (!data) return null;
    if (sortMode === "elo") return data;
    return [...data].sort((a, b) => {
      if (a.conference !== b.conference) return a.conference === "Est" ? -1 : 1;
      return (a.conferenceSeed ?? 99) - (b.conferenceSeed ?? 99);
    });
  }, [data, sortMode]);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="flex items-center gap-1.5 text-sm text-muted-foreground">
          <Trophy className="h-4 w-4 text-primary" />
          {sortMode === "elo"
            ? "Classement de force interne (Elo), recalculé au fil des résultats de la saison."
            : "Classement officiel NBA, par conférence."}
        </p>
        <div className="flex gap-2">
          <button type="button" onClick={() => setSortMode("elo")}>
            <Badge
              variant="outline"
              className={cn(
                "cursor-pointer border-transparent px-3 py-1.5 transition-colors",
                sortMode === "elo" ? "glass-accent" : "text-muted-foreground hover:text-foreground"
              )}
            >
              Force (Elo)
            </Badge>
          </button>
          <button type="button" onClick={() => setSortMode("official")}>
            <Badge
              variant="outline"
              className={cn(
                "cursor-pointer border-transparent px-3 py-1.5 transition-colors",
                sortMode === "official" ? "glass-accent" : "text-muted-foreground hover:text-foreground"
              )}
            >
              Classement officiel
            </Badge>
          </button>
        </div>
      </div>

      {isLoading && <BasketballLoader label="Chargement des équipes..." />}
      {isError && <p className="text-destructive">Impossible de charger les équipes.</p>}

      {sortedData && sortMode === "official" && (
        <div className="flex flex-col gap-6">
          {(["Est", "Ouest"] as const).map((conf) => (
            <div key={conf} className="flex flex-col gap-3">
              <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Conférence {conf}
              </p>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {sortedData
                  .filter((team) => team.conference === conf)
                  .map((team) => (
                    <TeamCard
                      key={team.id}
                      team={team}
                      badge={team.conferenceSeed !== null ? `#${team.conferenceSeed}` : "—"}
                      isExpanded={expandedTeamId === team.id}
                      onToggle={() => setExpandedTeamId(expandedTeamId === team.id ? null : team.id)}
                    />
                  ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {sortedData && sortMode === "elo" && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {sortedData.map((team) => (
            <TeamCard
              key={team.id}
              team={team}
              badge={`#${team.rank}`}
              isExpanded={expandedTeamId === team.id}
              onToggle={() => setExpandedTeamId(expandedTeamId === team.id ? null : team.id)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function TeamCard({
  team,
  badge,
  isExpanded,
  onToggle,
}: {
  team: TeamRank;
  badge: string;
  isExpanded: boolean;
  onToggle: () => void;
}) {
  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-6">
        <button type="button" onClick={onToggle} className="flex w-full items-center gap-3 text-left">
          <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={36} />
          <div className="min-w-0 flex-1">
            <p className="truncate font-medium">{team.name}</p>
            <p className="truncate text-xs text-muted-foreground">
              {team.conference} · {team.division}
            </p>
            {team.wins !== null && team.losses !== null && (
              <p className="truncate font-mono text-[11px] text-muted-foreground">
                {team.wins}-{team.losses}
                {team.streak && ` · série ${team.streak}`}
                {team.gamesBehind && team.gamesBehind !== "-" && ` · ${team.gamesBehind} GB`}
              </p>
            )}
          </div>
          <span className="shrink-0 rounded-full bg-primary/10 px-2 py-0.5 font-mono text-xs font-bold text-primary">
            {badge}
          </span>
        </button>

        {isExpanded && (
          <div className="border-t border-border pt-3">
            <TeamRoster teamId={team.id} teamName={team.name} />
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export default function PlayersPage() {
  const [tab, setTab] = useState<"players" | "teams">("players");

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-bold">Joueurs & équipes</h1>
        <p className="mt-1 text-muted-foreground">
          Cherche un joueur, actuel ou historique, ou explore le classement des équipes.
        </p>
      </div>

      <div className="flex gap-2">
        <button type="button" onClick={() => setTab("players")}>
          <Badge
            variant="outline"
            className={cn(
              "cursor-pointer border-transparent gap-1.5 px-3 py-1.5 transition-colors",
              tab === "players"
                ? "glass-accent"
                : "text-muted-foreground hover:text-foreground"
            )}
          >
            <Users className="h-3.5 w-3.5" />
            Joueurs
          </Badge>
        </button>
        <button type="button" onClick={() => setTab("teams")}>
          <Badge
            variant="outline"
            className={cn(
              "cursor-pointer border-transparent gap-1.5 px-3 py-1.5 transition-colors",
              tab === "teams"
                ? "glass-accent"
                : "text-muted-foreground hover:text-foreground"
            )}
          >
            <Shield className="h-3.5 w-3.5" />
            Équipes
          </Badge>
        </button>
      </div>

      {tab === "players" ? <PlayersTab /> : <TeamsTab />}
    </div>
  );
}
