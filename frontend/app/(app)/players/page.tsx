"use client";

import { use, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Activity, Crown, Search, Trophy, X } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import { PlayerCardDialog } from "@/components/player-card-dialog";
import { TeamLogo } from "@/components/team-logo";
import { InjuryPill, PlayerHeadshot, PlayerTile, playerName } from "@/components/players/player-tile";
import { PlayerCompareTray } from "@/components/players/player-compare";
import { fetchPlayerInjuries, fetchPlayerLeaders, fetchPlayers } from "@/lib/api/players";
import { fetchTeamRankings, fetchTeamRoster } from "@/lib/api/teams";
import { cn } from "@/lib/utils";
import { PlayerLeaders, RosterPlayer, TeamRank } from "@/lib/types";

const MIN_SEARCH_LENGTH = 2;
const MAX_COMPARED = 2;

function chipClass(active: boolean) {
  return cn(
    "flex shrink-0 items-center gap-1.5 rounded-full px-3 py-1.5 text-sm transition-colors",
    active ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
  );
}

const LEADER_CATEGORIES: { key: keyof PlayerLeaders; label: string; stat: (p: RosterPlayer) => number | null; suffix: string }[] = [
  { key: "points", label: "Points", stat: (p) => p.pointsPerGame, suffix: "pts" },
  { key: "rebounds", label: "Rebonds", stat: (p) => p.reboundsPerGame, suffix: "reb" },
  { key: "assists", label: "Passes", stat: (p) => p.assistsPerGame, suffix: "pd" },
  { key: "steals", label: "Interceptions", stat: (p) => p.stealsPerGame, suffix: "int" },
  { key: "blocks", label: "Contres", stat: (p) => p.blocksPerGame, suffix: "ctr" },
];

type Select = (p: RosterPlayer) => void;

/** Meneurs statistiques : le n°1 en grand (photo), les 4 suivants en liste. */
function LeadersSection({ onSelect }: { onSelect: Select }) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["players", "leaders"],
    queryFn: fetchPlayerLeaders,
    staleTime: 15 * 60 * 1000,
  });

  if (isLoading) return <BasketballLoader label="Chargement des meneurs..." />;
  if (isError || !data) return <p className="text-sm text-destructive">Impossible de charger les meneurs statistiques.</p>;

  return (
    <section className="flex flex-col gap-3">
      <h2 className="flex items-center gap-2 font-heading text-lg font-bold">
        <Crown className="h-5 w-5 text-primary" />
        Meneurs de la saison
      </h2>
      <div className="glass-scroll -mx-1 flex snap-x gap-3 overflow-x-auto px-1 pb-1">
        {LEADER_CATEGORIES.map(({ key, label, stat, suffix }) => {
          const [first, ...others] = data[key] ?? [];
          return (
            <Card key={key} className="w-64 shrink-0 snap-start">
              <CardContent className="flex flex-col gap-2 pt-5">
                <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{label}</p>
                {!first && <p className="text-sm text-muted-foreground">Pas encore de stats.</p>}
                {first && (
                  <button type="button" onClick={() => onSelect(first)} className="flex items-center gap-3 text-left">
                    <PlayerHeadshot player={first} size={64} />
                    <span className="min-w-0">
                      <span className="block truncate font-heading font-bold">{playerName(first)}</span>
                      <span className="flex items-center gap-1 text-xs text-muted-foreground">
                        {first.team && <TeamLogo abbreviation={first.team.abbreviation} logoUrl={first.team.logoUrl} size={14} />}
                        {first.team?.abbreviation}
                      </span>
                      <span className="font-mono text-lg font-bold text-primary">
                        {stat(first)?.toFixed(1)} <span className="text-xs">{suffix}</span>
                      </span>
                    </span>
                  </button>
                )}
                <div className="flex flex-col">
                  {others.map((p, i) => (
                    <button
                      key={p.id}
                      type="button"
                      onClick={() => onSelect(p)}
                      className="flex items-center gap-2 rounded-lg px-1 py-1 text-left text-sm transition-colors hover:bg-tint/5"
                    >
                      <span className="w-3 shrink-0 font-mono text-xs text-muted-foreground">{i + 2}</span>
                      <PlayerHeadshot player={p} size={24} />
                      <span className="min-w-0 flex-1 truncate">{playerName(p)}</span>
                      <span className="shrink-0 font-mono text-xs font-bold">{stat(p)?.toFixed(1)}</span>
                    </button>
                  ))}
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </section>
  );
}

/** Rapport des blessures, groupé par équipe : l'info clé avant de parier. */
function InjuriesSection({ onSelect }: { onSelect: Select }) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["players", "injuries"],
    queryFn: fetchPlayerInjuries,
    staleTime: 15 * 60 * 1000,
  });
  const [showAll, setShowAll] = useState(false);

  const byTeam = useMemo(() => {
    const groups = new Map<string, RosterPlayer[]>();
    for (const p of data ?? []) {
      const key = p.team?.abbreviation ?? "?";
      groups.set(key, [...(groups.get(key) ?? []), p]);
    }
    return [...groups.entries()];
  }, [data]);
  const visible = showAll ? byTeam : byTeam.slice(0, 6);
  const outCount = (data ?? []).filter((p) => p.injuryStatus?.toLowerCase() === "out").length;

  return (
    <section className="flex flex-col gap-3">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="flex items-center gap-2 font-heading text-lg font-bold">
          <Activity className="h-5 w-5 text-destructive" />
          Blessés
        </h2>
        {data && (
          <p className="text-xs text-muted-foreground">
            {outCount} absent{outCount > 1 ? "s" : ""} · {data.length - outCount} incertain{data.length - outCount > 1 ? "s" : ""}
          </p>
        )}
      </div>
      {isLoading && <BasketballLoader label="Chargement des blessés..." />}
      {isError && <p className="text-sm text-destructive">Impossible de charger le rapport des blessures.</p>}
      {data && data.length === 0 && <p className="text-sm text-muted-foreground">Aucun joueur blessé signalé.</p>}
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        {visible.map(([abbr, players]) => (
          <Card key={abbr}>
            <CardContent className="flex flex-col gap-1.5 pt-4">
              <p className="mb-1 flex items-center gap-2 text-sm font-semibold">
                {players[0].team && <TeamLogo abbreviation={abbr} logoUrl={players[0].team.logoUrl} size={22} />}
                {players[0].team?.name ?? "Sans équipe"}
              </p>
              {players.map((p) => (
                <button
                  key={p.id}
                  type="button"
                  onClick={() => onSelect(p)}
                  className="flex items-center gap-2 rounded-lg px-1 py-1 text-left text-sm transition-colors hover:bg-tint/5"
                >
                  <PlayerHeadshot player={p} size={24} />
                  <span className="min-w-0 flex-1 truncate">{playerName(p)}</span>
                  <InjuryPill status={p.injuryStatus} />
                </button>
              ))}
            </CardContent>
          </Card>
        ))}
      </div>
      {byTeam.length > 6 && (
        <button type="button" onClick={() => setShowAll((v) => !v)} className="self-start text-sm font-medium text-primary hover:underline">
          {showAll ? "Voir moins" : `Voir les ${byTeam.length} équipes`}
        </button>
      )}
    </section>
  );
}

function PlayerGrid({
  players,
  onSelect,
  compared,
  onCompare,
}: {
  players: RosterPlayer[];
  onSelect: Select;
  compared: RosterPlayer[];
  onCompare: Select;
}) {
  return (
    <div className="stagger-children grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
      {players.map((p) => (
        <PlayerTile
          key={p.id}
          player={p}
          onSelect={onSelect}
          onCompare={onCompare}
          inComparison={compared.some((c) => c.id === p.id)}
        />
      ))}
    </div>
  );
}

function PlayersTab() {
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [position, setPosition] = useState<string | null>(null);
  const [teamId, setTeamId] = useState<string | null>(null);
  const [selected, setSelected] = useState<RosterPlayer | null>(null);
  const [compared, setCompared] = useState<RosterPlayer[]>([]);

  useEffect(() => {
    const id = window.setTimeout(() => setDebouncedSearch(search), 300);
    return () => window.clearTimeout(id);
  }, [search]);

  const isSearching = debouncedSearch.trim().length >= MIN_SEARCH_LENGTH;

  const teamsQuery = useQuery({ queryKey: ["teams", "rankings"], queryFn: fetchTeamRankings, staleTime: 60 * 60 * 1000 });
  const sortedTeams = useMemo(
    () => [...(teamsQuery.data ?? [])].sort((a, b) => a.name.localeCompare(b.name)),
    [teamsQuery.data]
  );
  const selectedTeam = sortedTeams.find((t) => t.id === teamId);

  const searchQuery = useQuery({
    queryKey: ["players", "search", debouncedSearch],
    queryFn: () => fetchPlayers({ search: debouncedSearch }),
    enabled: isSearching,
    staleTime: 60 * 60 * 1000,
  });
  const rosterQuery = useQuery({
    queryKey: ["team-roster", teamId],
    queryFn: () => fetchTeamRoster(teamId!),
    enabled: !!teamId && !isSearching,
    staleTime: 60 * 60 * 1000,
  });

  const activeQuery = isSearching ? searchQuery : teamId ? rosterQuery : null;
  const activeData = activeQuery?.data;
  const results = useMemo(() => activeData ?? [], [activeData]);

  // Postes réellement présents dans les résultats ("G", "G-F"… selon la
  // source) plutôt qu'une liste figée. Un poste qui n'existe plus dans les
  // nouveaux résultats est simplement ignoré : pas besoin de le réinitialiser.
  const positions = useMemo(() => [...new Set(results.map((p) => p.position).filter(Boolean) as string[])].sort(), [results]);
  const effectivePosition = position && positions.includes(position) ? position : null;
  const shown = effectivePosition ? results.filter((p) => p.position === effectivePosition) : results;

  function toggleCompare(p: RosterPlayer) {
    setCompared((prev) => {
      if (prev.some((c) => c.id === p.id)) return prev.filter((c) => c.id !== p.id);
      // Déjà deux joueurs : le nouveau remplace le plus ancien.
      return [...prev, p].slice(-MAX_COMPARED);
    });
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3">
        <div className="relative max-w-md">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Chercher un joueur (nom ou prénom)…"
            aria-label="Chercher un joueur"
            className="pl-9"
          />
        </div>
        {!isSearching && sortedTeams.length > 0 && (
          <div className="glass-scroll flex items-center gap-1.5 overflow-x-auto pb-0.5" role="group" aria-label="Parcourir une équipe">
            <span className="mr-1 shrink-0 text-xs text-muted-foreground">ou parcours une équipe :</span>
            {sortedTeams.map((t) => (
              <button
                key={t.id}
                type="button"
                title={t.name}
                aria-label={t.name}
                aria-pressed={teamId === t.id}
                onClick={() => setTeamId(teamId === t.id ? null : t.id)}
                className={cn(
                  "flex shrink-0 items-center justify-center rounded-full p-1.5 transition-all",
                  teamId === t.id ? "glass-accent" : teamId ? "opacity-40 hover:opacity-100" : "hover:bg-tint/[0.06]"
                )}
              >
                <TeamLogo abbreviation={t.abbreviation} logoUrl={t.logoUrl} size={26} />
              </button>
            ))}
          </div>
        )}
      </div>

      {activeQuery && (
        <section className="flex flex-col gap-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="flex items-center gap-2 font-heading text-lg font-bold">
              {isSearching ? (
                <>Résultats pour « {debouncedSearch.trim()} »</>
              ) : (
                selectedTeam && (
                  <>
                    <TeamLogo abbreviation={selectedTeam.abbreviation} logoUrl={selectedTeam.logoUrl} size={28} />
                    Effectif : {selectedTeam.name}
                    <button type="button" onClick={() => setTeamId(null)} aria-label="Fermer l'effectif">
                      <X className="h-4 w-4 text-muted-foreground hover:text-foreground" />
                    </button>
                  </>
                )
              )}
            </h2>
            {!isSearching && selectedTeam && (
              <Link href={`/teams/${selectedTeam.id}`} className="text-sm font-medium text-primary hover:underline">
                Page de l&apos;équipe
              </Link>
            )}
          </div>

          {positions.length > 1 && (
            <div className="flex flex-wrap gap-2">
              <button type="button" className={chipClass(effectivePosition === null)} onClick={() => setPosition(null)}>
                Tous
              </button>
              {positions.map((pos) => (
                <button key={pos} type="button" className={cn(chipClass(effectivePosition === pos), "font-mono")} onClick={() => setPosition(pos)}>
                  {pos}
                </button>
              ))}
            </div>
          )}

          {activeQuery.isLoading && <BasketballLoader label="Chargement…" />}
          {activeQuery.isError && <p className="text-destructive">Impossible de charger les joueurs.</p>}
          {!activeQuery.isLoading && !activeQuery.isError && results.length === 0 && (
            <p className="text-muted-foreground">
              {isSearching ? `Aucun joueur trouvé pour « ${debouncedSearch.trim()} ».` : "Effectif indisponible."}
            </p>
          )}
          <PlayerGrid players={shown} onSelect={setSelected} compared={compared} onCompare={toggleCompare} />
        </section>
      )}

      {!activeQuery && (
        <>
          <LeadersSection onSelect={setSelected} />
          <InjuriesSection onSelect={setSelected} />
          <p className="text-xs text-muted-foreground">
            Astuce : cherche ou parcours une équipe, puis utilise le bouton de comparaison sur deux joueurs pour les comparer.
          </p>
        </>
      )}

      <PlayerCardDialog player={selected} onOpenChange={(open) => !open && setSelected(null)} />
      <PlayerCompareTray
        players={compared}
        onRemove={(p) => setCompared((prev) => prev.filter((c) => c.id !== p.id))}
        onClear={() => setCompared([])}
      />
    </div>
  );
}

/* ── Onglet Équipes ─────────────────────────────────────────────────── */

type TeamSortMode = "official" | "elo";

function winPct(t: TeamRank): string {
  if (t.wins === null || t.losses === null || t.wins + t.losses === 0) return "-";
  return (t.wins / (t.wins + t.losses)).toFixed(3).replace(/^0/, "");
}

/** Classement officiel d'une conférence, avec les lignes Playoffs (1-6) et Play-in (7-10). */
function StandingsTable({ conference, teams }: { conference: string; teams: TeamRank[] }) {
  const rows = [...teams].sort((a, b) => (a.conferenceSeed ?? 99) - (b.conferenceSeed ?? 99));
  return (
    <Card className="overflow-hidden">
      <CardContent className="px-0 pt-4">
        <p className="mb-2 px-4 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Conférence {conference}</p>
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-[11px] uppercase text-muted-foreground">
              <th className="w-10 px-4 py-1 font-medium">#</th>
              <th className="py-1 font-medium">Équipe</th>
              <th className="px-2 py-1 text-right font-medium">V</th>
              <th className="px-2 py-1 text-right font-medium">D</th>
              <th className="hidden px-2 py-1 text-right font-medium sm:table-cell">%</th>
              <th className="hidden px-2 py-1 text-right font-medium sm:table-cell">Écart</th>
              <th className="px-4 py-1 text-right font-medium">Série</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((t) => {
              const seed = t.conferenceSeed ?? 0;
              // Liseré sous la 6e (fin des places directes) et la 10e (fin du play-in).
              const divider = seed === 6 || seed === 10;
              return (
                <tr
                  key={t.id}
                  className={cn("transition-colors hover:bg-tint/[0.04]", divider && "shadow-[inset_0_-2px_0_var(--hairline-strong)]")}
                >
                  <td className={cn("px-4 py-2 font-mono font-bold", seed <= 6 ? "text-success" : seed <= 10 ? "text-primary" : "text-muted-foreground")}>
                    {t.conferenceSeed ?? "-"}
                  </td>
                  <td className="py-2">
                    <Link href={`/teams/${t.id}`} className="flex items-center gap-2 font-medium hover:underline">
                      <TeamLogo abbreviation={t.abbreviation} logoUrl={t.logoUrl} size={24} />
                      <span className="truncate">{t.name}</span>
                    </Link>
                  </td>
                  <td className="px-2 py-2 text-right font-mono">{t.wins ?? "-"}</td>
                  <td className="px-2 py-2 text-right font-mono">{t.losses ?? "-"}</td>
                  <td className="hidden px-2 py-2 text-right font-mono text-muted-foreground sm:table-cell">{winPct(t)}</td>
                  <td className="hidden px-2 py-2 text-right font-mono text-muted-foreground sm:table-cell">
                    {t.gamesBehind && t.gamesBehind !== "-" ? t.gamesBehind : "-"}
                  </td>
                  <td className="px-4 py-2 text-right font-mono text-xs">{t.streak ?? "-"}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
        <p className="flex flex-wrap gap-x-4 gap-y-1 px-4 pt-3 text-[11px] text-muted-foreground">
          <span><span className="font-bold text-success">1-6</span> qualifiés pour les playoffs</span>
          <span><span className="font-bold text-primary">7-10</span> play-in</span>
        </p>
      </CardContent>
    </Card>
  );
}

function TeamsTab() {
  const [sortMode, setSortMode] = useState<TeamSortMode>("official");
  const { data, isLoading, isError } = useQuery({
    queryKey: ["teams", "rankings"],
    queryFn: fetchTeamRankings,
    staleTime: 5 * 60 * 1000,
  });

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="flex items-center gap-1.5 text-sm text-muted-foreground">
          <Trophy className="h-4 w-4 text-primary" />
          {sortMode === "elo"
            ? "Force interne (Elo), celle qui sert au calcul des cotes."
            : "Classement officiel NBA, par conférence."}
        </p>
        <div className="flex gap-2">
          <button type="button" className={chipClass(sortMode === "official")} onClick={() => setSortMode("official")}>
            Classement officiel
          </button>
          <button type="button" className={chipClass(sortMode === "elo")} onClick={() => setSortMode("elo")}>
            Force (Elo)
          </button>
        </div>
      </div>

      {isLoading && <BasketballLoader label="Chargement des équipes..." />}
      {isError && <p className="text-destructive">Impossible de charger les équipes.</p>}

      {data && sortMode === "official" && (
        <div className="grid gap-4 xl:grid-cols-2">
          {(["Est", "Ouest"] as const).map((conf) => (
            <StandingsTable key={conf} conference={conf} teams={data.filter((t) => t.conference === conf)} />
          ))}
        </div>
      )}

      {data && sortMode === "elo" && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {data.map((team) => (
            <Link key={team.id} href={`/teams/${team.id}`}>
              <Card className="transition-colors hover:bg-tint/5">
                <CardContent className="flex items-center gap-3 pt-6">
                  <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={36} />
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium">{team.name}</p>
                    <p className="truncate font-mono text-xs text-muted-foreground">Elo {Math.round(team.eloRating)}</p>
                  </div>
                  <span className="shrink-0 rounded-full bg-primary/10 px-2 py-0.5 font-mono text-xs font-bold text-primary">
                    #{team.rank}
                  </span>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

export default function PlayersPage({ searchParams }: { searchParams: Promise<{ tab?: string }> }) {
  // Joueurs et Équipes sont deux onglets de la rubrique NBA (SectionTabs),
  // distingués par l'adresse (?tab=equipes) : partageable et retrouvé au retour.
  const { tab } = use(searchParams);
  const teams = tab === "equipes";

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-bold">{teams ? "Équipes" : "Joueurs"}</h1>
        <p className="mt-1 text-muted-foreground">
          {teams
            ? "Classement officiel par conférence, effectifs et classement Elo des 30 équipes."
            : "Meneurs statistiques, blessés, recherche et comparateur de joueurs."}
        </p>
      </div>

      {teams ? <TeamsTab /> : <PlayersTab />}
    </div>
  );
}
