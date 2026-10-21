"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ChevronDown, Flame, Search, Users } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { OddsButton, OddsPreviewContext } from "@/components/odds-button";
import { TeamLogo } from "@/components/team-logo";
import { useBetSlip } from "@/components/bet-slip-provider";
import { DEFAULT_PAST_DAYS, fetchMatches, fetchPlayerPropsOf } from "@/lib/api/matches";
import { PLAYER_PROP_MARKETS, decimal, propSelection } from "@/lib/player-props";
import { useTimeZone } from "@/lib/use-time-zone";
import type { Match, PlayerProp, PlayerPropMarket } from "@/lib/types";
import { bettingClosedReason, cn, formatMatchTime, formatNightChip, isBettable, nightKey, opensLater } from "@/lib/utils";

const MARKETS = Object.keys(PLAYER_PROP_MARKETS) as PlayerPropMarket[];
// Paris visibles d'un joueur avant « Voir plus » : des cartes de même
// hauteur côte à côte, au lieu de colonnes en escalier.
const VISIBLE_PROPS = 3;

function chipClass(active: boolean) {
  return cn(
    "shrink-0 whitespace-nowrap rounded-full px-3 py-1.5 text-xs font-semibold transition-colors",
    active ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
  );
}

interface PlayerGroup {
  playerId: string;
  playerName: string;
  teamAbbreviation: string;
  headshotUrl: string | null;
  injuryStatus: string | null;
  match: Match;
  props: PlayerProp[];
}

function PropLine({ match, prop }: { match: Match; prop: PlayerProp }) {
  return (
    <div className="flex items-center gap-2">
      <span className="w-24 shrink-0 text-xs font-semibold text-muted-foreground">
        {PLAYER_PROP_MARKETS[prop.market].tab}
        <span className="block font-mono text-[10px] font-normal">moy. {decimal(prop.average, 1)}</span>
      </span>
      <div className="flex flex-1 gap-1">
        <OddsButton
          selection={propSelection(match, prop, "over")}
          label={`+ de ${decimal(prop.line)}`}
          communityPct={prop.overPct}
        />
        <OddsButton
          selection={propSelection(match, prop, "under")}
          label={`− de ${decimal(prop.line)}`}
          communityPct={prop.overPct === null ? null : 100 - prop.overPct}
        />
      </div>
    </div>
  );
}

function PlayerCard({ group, slipMatchIds }: { group: PlayerGroup; slipMatchIds: Map<string, string> }) {
  const { match } = group;
  const atHome = match.homeTeam.abbreviation === group.teamAbbreviation;
  const team = atHome ? match.homeTeam : match.awayTeam;
  const opponent = atHome ? match.awayTeam : match.homeTeam;
  const otherSelection = slipMatchIds.get(match.id);
  const preview = !isBettable(match);
  const [expanded, setExpanded] = useState(false);
  const hidden = group.props.length - VISIBLE_PROPS;
  const shown = expanded ? group.props : group.props.slice(0, VISIBLE_PROPS);

  return (
    <OddsPreviewContext.Provider value={preview}>
      <Card className="h-full">
        <CardContent className="flex flex-1 flex-col gap-3 pt-5">
          <div className="flex items-center gap-3">
            {group.headshotUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={group.headshotUrl} alt="" className="h-12 w-12 shrink-0 rounded-full bg-tint/10 object-cover" />
            ) : (
              <span className="h-12 w-12 shrink-0 rounded-full bg-tint/10" aria-hidden />
            )}
            <div className="min-w-0 flex-1">
              <p className="truncate font-semibold">{group.playerName}</p>
              <Link
                href={`/matches/${match.id}`}
                className="flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground"
              >
                <TeamLogo abbreviation={group.teamAbbreviation} logoUrl={team.logoUrl} size={14} />
                {group.teamAbbreviation} {atHome ? "contre" : "à"} {opponent.abbreviation} · {formatMatchTime(new Date(match.date))}
              </Link>
            </div>
            {group.injuryStatus && (
              <span className="shrink-0 rounded-full bg-amber-500/15 px-2 py-0.5 text-[10px] font-bold uppercase text-amber-600 dark:text-amber-400">
                {group.injuryStatus}
              </span>
            )}
          </div>
          <div className="flex flex-col gap-1.5">
            {shown.map((prop) => (
              <PropLine key={prop.market} match={match} prop={prop} />
            ))}
          </div>
          {hidden > 0 && (
            <button
              type="button"
              onClick={() => setExpanded((v) => !v)}
              aria-expanded={expanded}
              className="mt-auto flex items-center justify-center gap-1 rounded-xl py-1 text-xs font-semibold text-muted-foreground transition-colors hover:text-foreground"
            >
              {expanded ? "Moins de paris" : `${hidden} autre${hidden > 1 ? "s" : ""} pari${hidden > 1 ? "s" : ""}`}
              <ChevronDown className={cn("h-3.5 w-3.5 transition-transform", expanded && "rotate-180")} />
            </button>
          )}
          {preview && <p className="text-[11px] text-muted-foreground first-letter:uppercase">{bettingClosedReason(match)}.</p>}
          {!preview && otherSelection && !otherSelection.includes(group.playerId) && (
            <p className="text-[11px] text-muted-foreground">
              Un choix ici remplace ta sélection sur {match.awayTeam.abbreviation} @ {match.homeTeam.abbreviation} (une par match et par ticket).
            </p>
          )}
        </CardContent>
      </Card>
    </OddsPreviewContext.Provider>
  );
}

export default function PlayerPropsPage() {
  const timeZone = useTimeZone();
  const { selections } = useBetSlip();
  const [chosenNight, setChosenNight] = useState<string | null>(null);
  const [market, setMarket] = useState<PlayerPropMarket | "all">("all");
  const [team, setTeam] = useState<string | null>(null);
  const [query, setQuery] = useState("");

  const matchesQuery = useQuery({
    queryKey: ["matches", "window", DEFAULT_PAST_DAYS],
    queryFn: () => fetchMatches(),
    refetchInterval: 60 * 1000,
  });

  // Soirées NBA à venir qui ont des matchs à parier (ou en aperçu), présaison exclue.
  const nights = useMemo(() => {
    const map = new Map<string, Match[]>();
    for (const m of matchesQuery.data ?? []) {
      if (m.type === "preseason" || !(isBettable(m) || opensLater(m))) continue;
      const key = nightKey(new Date(m.date), timeZone);
      map.set(key, [...(map.get(key) ?? []), m]);
    }
    for (const list of map.values()) list.sort((a, b) => a.date.localeCompare(b.date));
    return new Map([...map.entries()].sort(([a], [b]) => a.localeCompare(b)));
  }, [matchesQuery.data, timeZone]);
  const nightKeys = [...nights.keys()];
  const night = chosenNight && nights.has(chosenNight) ? chosenNight : nightKeys[0] ?? null;
  const nightMatches = useMemo(() => (night ? nights.get(night) ?? [] : []).slice(0, 20), [night, nights]);
  const matchById = useMemo(() => new Map(nightMatches.map((m) => [m.id, m])), [nightMatches]);

  const propsQuery = useQuery({
    queryKey: ["player-props", "night", nightMatches.map((m) => m.id).join(",")],
    queryFn: () => fetchPlayerPropsOf(nightMatches.map((m) => m.id)),
    enabled: nightMatches.length > 0,
    staleTime: 5 * 60 * 1000,
  });
  const props = useMemo(() => propsQuery.data ?? [], [propsQuery.data]);

  const marketsPresent = MARKETS.filter((m) => props.some((p) => p.market === m));
  const teamsPresent = [...new Set(props.map((p) => p.teamAbbreviation))].sort();
  const activeMarket = market !== "all" && marketsPresent.includes(market) ? market : "all";
  const activeTeam = team && teamsPresent.includes(team) ? team : null;

  const groups = useMemo(() => {
    const q = query.trim().toLowerCase();
    const map = new Map<string, PlayerGroup>();
    for (const p of props) {
      const match = matchById.get(p.matchId);
      if (!match) continue;
      if (activeMarket !== "all" && p.market !== activeMarket) continue;
      if (activeTeam && p.teamAbbreviation !== activeTeam) continue;
      if (q && !p.playerName.toLowerCase().includes(q)) continue;
      const g = map.get(p.playerId) ?? {
        playerId: p.playerId,
        playerName: p.playerName,
        teamAbbreviation: p.teamAbbreviation,
        headshotUrl: p.headshotUrl,
        injuryStatus: p.injuryStatus,
        match,
        props: [],
      };
      g.props.push(p);
      map.set(p.playerId, g);
    }
    const order = (pr: PlayerProp) => MARKETS.indexOf(pr.market);
    const points = (g: PlayerGroup) => g.props.find((pr) => pr.market === "player_points")?.average ?? 0;
    return [...map.values()]
      .map((g) => ({ ...g, props: [...g.props].sort((a, b) => order(a) - order(b)) }))
      .sort((a, b) => a.match.date.localeCompare(b.match.date) || points(b) - points(a));
  }, [props, matchById, activeMarket, activeTeam, query]);

  const popular = useMemo(
    () => [...props].filter((p) => p.picks > 0).sort((a, b) => b.picks - a.picks).slice(0, 5),
    [props]
  );

  // Sélection du ticket par match : pour prévenir qu'un nouveau choix remplace l'ancien.
  const slipMatchIds = useMemo(() => new Map(selections.map((s) => [s.matchId, s.id])), [selections]);

  return (
    <div className="flex flex-col gap-5">
      <div>
        <h1 className="font-heading text-2xl font-bold">Paris joueurs</h1>
        <p className="mt-1 text-muted-foreground">
          Tous les paris joueurs de la soirée : points, rebonds, passes, tirs à 3 points, contres et cumuls.
        </p>
      </div>

      {matchesQuery.isError && <p className="text-destructive">Impossible de charger les matchs. Réessaie plus tard.</p>}

      <div className="glass-chrome bleed py-3">
        <div className="glass-scroll flex gap-2 overflow-x-auto pb-0.5" role="tablist" aria-label="Soirée">
          {matchesQuery.isLoading &&
            Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-14 w-16 shrink-0 rounded-2xl" />)}
          {nightKeys.map((key) => {
            const first = nights.get(key)![0];
            const { top, bottom } = formatNightChip(new Date(first.date), timeZone);
            const active = key === night;
            return (
              <button
                key={key}
                type="button"
                role="tab"
                aria-selected={active}
                onClick={() => setChosenNight(key)}
                className={cn(
                  "flex shrink-0 flex-col items-center rounded-2xl px-3 py-1.5 transition-colors",
                  active ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
                )}
              >
                <span className="text-[11px] font-semibold capitalize">{top}</span>
                <span className="text-sm font-bold">{bottom}</span>
                <span className="font-mono text-[10px] opacity-80">
                  {nights.get(key)!.length} match{nights.get(key)!.length > 1 ? "s" : ""}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {!matchesQuery.isLoading && nightKeys.length === 0 && (
        <p className="glass-inset-quiet rounded-xl px-3 py-6 text-center text-sm text-muted-foreground">
          Aucun match à venir pour l&apos;instant.
        </p>
      )}

      {night && (
        <>
          <div className="flex flex-col gap-2">
            <div className="glass-scroll flex gap-1.5 overflow-x-auto pb-0.5" role="tablist" aria-label="Statistique">
              <button type="button" className={chipClass(activeMarket === "all")} onClick={() => setMarket("all")}>
                Toutes
              </button>
              {marketsPresent.map((m) => (
                <button key={m} type="button" className={chipClass(activeMarket === m)} onClick={() => setMarket(m)}>
                  {PLAYER_PROP_MARKETS[m].tab}
                </button>
              ))}
            </div>
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  type="search"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Joueur…"
                  aria-label="Rechercher un joueur"
                  className="pl-9"
                />
              </div>
              <select
                value={activeTeam ?? ""}
                onChange={(e) => setTeam(e.target.value || null)}
                aria-label="Équipe"
                className="glass-inset rounded-xl px-3 text-sm"
              >
                <option value="">Toutes les équipes</option>
                {teamsPresent.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {popular.length > 0 && activeMarket === "all" && !activeTeam && !query && (
            <section className="flex flex-col gap-2">
              <h2 className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wide text-muted-foreground">
                <Flame className="h-3.5 w-3.5 text-primary" />
                Les plus joués
              </h2>
              <div className="flex flex-col gap-1.5">
                {popular.map((p) => {
                  const match = matchById.get(p.matchId)!;
                  const over = p.overPct ?? 50;
                  return (
                    <OddsPreviewContext.Provider key={`${p.matchId}-${p.market}-${p.playerId}`} value={!isBettable(match)}>
                      <div className="glass-inset-quiet flex items-center gap-3 rounded-xl px-3 py-2">
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-semibold">
                            {p.playerName} · {PLAYER_PROP_MARKETS[p.market].tab} {decimal(p.line)}
                          </p>
                          <p className="flex items-center gap-1 text-[11px] text-muted-foreground">
                            <Users className="h-3 w-3" />
                            {over >= 50 ? `${over} % sur plus` : `${100 - over} % sur moins`} · {p.picks} choix
                          </p>
                        </div>
                        <div className="flex w-[8.5rem] shrink-0 gap-1">
                          <OddsButton selection={propSelection(match, p, "over")} label={`+ de ${decimal(p.line)}`} />
                          <OddsButton selection={propSelection(match, p, "under")} label={`− de ${decimal(p.line)}`} />
                        </div>
                      </div>
                    </OddsPreviewContext.Provider>
                  );
                })}
              </div>
            </section>
          )}

          {propsQuery.isLoading && <Skeleton className="h-48 w-full rounded-2xl" />}
          {propsQuery.isError && <p className="text-destructive">Impossible de charger les paris joueurs.</p>}
          {propsQuery.data && groups.length === 0 && (
            <p className="glass-inset-quiet rounded-xl px-3 py-6 text-center text-sm text-muted-foreground">
              {props.length === 0 ? "Pas encore de paris joueurs pour cette soirée." : "Aucun joueur ne correspond."}
            </p>
          )}
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            {groups.map((g) => (
              <PlayerCard key={g.playerId} group={g} slipMatchIds={slipMatchIds} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
