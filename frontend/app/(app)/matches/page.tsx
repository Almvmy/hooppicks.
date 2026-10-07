"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { CalendarDays, FlaskConical, Heart, Radio, SlidersHorizontal, Trophy, X } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { MatchCard } from "@/components/match-card";
import { FinishedMatchRow } from "@/components/finished-match-row";
import { NbaLogo } from "@/components/nba-logo";
import { TeamLogo } from "@/components/team-logo";
import { MATCH_TYPE_META } from "@/components/match-stage-badge";
import { PlayoffSeries } from "@/components/playoff-series";
import { fetchMatches } from "@/lib/api/matches";
import { fetchProfile } from "@/lib/api/auth";
import { useTimeZone } from "@/lib/use-time-zone";
import { favoriteTeamAbbreviation, useTeamsByAbbreviation } from "@/lib/use-teams";
import { Conference, Match, MatchStatus, MatchType } from "@/lib/types";
import { cn, dayKey, formatDayChip, getDayLabel } from "@/lib/utils";

const CONFERENCES: (Conference | "Toutes")[] = ["Toutes", "Est", "Ouest"];
const STATUSES: { value: MatchStatus | "all"; label: string }[] = [
  { value: "all", label: "Tous" },
  { value: "scheduled", label: "À venir" },
  { value: "live", label: "En direct" },
  { value: "finished", label: "Terminés" },
];

// Ordre chronologique de la saison, pour les pastilles de phase.
const PHASES: MatchType[] = ["preseason", "regular", "nba_cup", "all_star", "play_in", "playoffs"];
// Un match pas encore relié à ESPN est traité comme de la saison régulière.
const phaseOf = (m: Match): MatchType => m.type ?? "regular";

function chipClass(active: boolean) {
  return cn(
    "flex shrink-0 items-center gap-1.5 rounded-full px-3 py-1.5 text-sm transition-colors",
    active ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
  );
}

export default function MatchesPage() {
  const timeZone = useTimeZone();
  const [conference, setConference] = useState<Conference | "Toutes">("Toutes");
  const [status, setStatus] = useState<MatchStatus | "all">("all");
  const [team, setTeam] = useState<string | null>(null);
  const [phase, setPhase] = useState<MatchType | "all">("all");
  const [chosenDay, setChosenDay] = useState<string | null>(null);
  // Phase, conférence et équipe repliées derrière « Filtres » : trois rangées
  // de pastilles passaient avant le premier match sur mobile.
  const [showFilters, setShowFilters] = useState(false);

  const { data, isLoading, isError, dataUpdatedAt } = useQuery({
    queryKey: ["matches"],
    queryFn: fetchMatches,
    refetchInterval: 60 * 1000, // scores et statuts en direct : on suit
  });
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const teams = useTeamsByAbbreviation();
  const favorite = favoriteTeamAbbreviation(profile?.favoriteTeam, teams);
  const sortedTeams = useMemo(() => [...teams.values()].sort((a, b) => a.name.localeCompare(b.name)), [teams]);

  // Pastilles de phase seulement s'il y a autre chose que de la saison
  // régulière dans le calendrier chargé.
  const phases = useMemo(() => {
    const present = new Set((data ?? []).map(phaseOf));
    return PHASES.filter((p) => present.has(p));
  }, [data]);
  const showPhases = phases.some((p) => p !== "regular");
  const activePhase = phase !== "all" && phases.includes(phase) ? phase : "all";

  // Filtres phase + conférence + équipe (le statut est appliqué à part : les matchs
  // en direct ont leur propre bandeau épinglé).
  const scoped = useMemo(() => {
    return (data ?? []).filter((m) => {
      if (activePhase !== "all" && phaseOf(m) !== activePhase) return false;
      if (conference !== "Toutes" && m.homeTeam.conference !== conference && m.awayTeam.conference !== conference) {
        return false;
      }
      if (team && m.homeTeam.abbreviation !== team && m.awayTeam.abbreviation !== team) return false;
      return true;
    });
  }, [data, conference, team, activePhase]);

  const live = status === "all" || status === "live" ? scoped.filter((m) => m.status === "live") : [];

  // Prochain match de ton équipe dans les 3 jours, épinglé en haut (hors
  // filtre d'équipe : là, il est déjà en tête de liste). « Maintenant » =
  // heure de chargement des matchs, pour garder un rendu pur.
  const favoriteNext = useMemo(() => {
    if (!favorite) return undefined;
    return (data ?? [])
      .filter((m) => {
        const t = new Date(m.date).getTime();
        return (
          m.status === "scheduled" &&
          (m.homeTeam.abbreviation === favorite || m.awayTeam.abbreviation === favorite) &&
          t > dataUpdatedAt &&
          t - dataUpdatedAt < 3 * 24 * 60 * 60 * 1000
        );
      })
      .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())[0];
  }, [data, favorite, dataUpdatedAt]);

  // Matchs par jour (dans le fuseau d'affichage), hors direct déjà épinglé.
  const byDay = useMemo(() => {
    const map = new Map<string, Match[]>();
    for (const m of scoped) {
      if (m.status === "live") continue;
      if (status !== "all" && m.status !== status) continue;
      const key = dayKey(new Date(m.date), timeZone);
      map.set(key, [...(map.get(key) ?? []), m]);
    }
    for (const list of map.values()) list.sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
    return new Map([...map.entries()].sort(([a], [b]) => a.localeCompare(b)));
  }, [scoped, status, timeZone]);

  const days = [...byDay.keys()];
  const today = dayKey(new Date(), timeZone);
  // Jour affiché par défaut : aujourd'hui s'il a des matchs, sinon le prochain
  // jour de match, sinon le dernier (fin de saison). Calculé plutôt que stocké :
  // il reste juste quand un filtre fait disparaître le jour choisi.
  const defaultDay = days.find((d) => d >= today) ?? days[days.length - 1] ?? null;
  const selectedDay = chosenDay && byDay.has(chosenDay) ? chosenDay : defaultDay;
  const dayMatches = selectedDay ? byDay.get(selectedDay) ?? [] : [];
  // Ce qu'on peut encore parier d'abord, les matchs terminés à la fin.
  const dayOpen = dayMatches.filter((m) => m.status !== "finished");
  const dayFinished = dayMatches.filter((m) => m.status === "finished");
  const noMatchToday = !isLoading && days.length > 0 && !byDay.has(today) && selectedDay === defaultDay && selectedDay !== null && selectedDay > today;

  // La pastille sélectionnée reste visible dans la bande (au chargement,
  // ou quand le jour par défaut change avec un filtre).
  // Défilement calculé dans la bande elle-même : scrollIntoView ferait aussi
  // défiler la page, et le défilement de la page interrompait celui de la
  // bande (le jour choisi restait hors champ sur mobile).
  const stripRef = useRef<HTMLDivElement>(null);
  const stripPositioned = useRef(false);
  useEffect(() => {
    const strip = stripRef.current;
    const chip = strip?.querySelector<HTMLElement>(`[data-day="${selectedDay}"]`);
    if (!strip || !chip) return;
    strip.scrollTo({
      left: chip.offsetLeft - (strip.clientWidth - chip.clientWidth) / 2,
      behavior: stripPositioned.current ? "smooth" : "auto",
    });
    stripPositioned.current = true;
  }, [selectedDay, days.length]);

  const hasFilters = conference !== "Toutes" || status !== "all" || team !== null || activePhase !== "all";
  const secondaryFilterCount = (conference !== "Toutes" ? 1 : 0) + (team !== null ? 1 : 0) + (activePhase !== "all" ? 1 : 0);
  function resetFilters() {
    setPhase("all");
    setConference("Toutes");
    setStatus("all");
    setTeam(null);
  }

  return (
    <div className="flex flex-col gap-5">
      <div>
        <h1 className="flex items-center gap-2.5 font-heading text-2xl font-bold">
          <NbaLogo size={30} />
          Matchs
        </h1>
        <p className="mt-1 text-muted-foreground">Calendrier de la saison NBA, jour par jour.</p>
      </div>

      {/* Seule la bande des jours reste collée sous la topbar (à partir de la
          tablette) : avec les filtres, le bandeau collé faisait quatre rangées
          et masquait une bonne partie des cartes au défilement. */}
      <div className="glass-chrome -mx-6 px-6 py-3 md:sticky md:top-16 md:z-20">
        <div ref={stripRef} className="glass-scroll relative flex gap-2 overflow-x-auto pb-0.5" role="tablist" aria-label="Jour">
          {isLoading &&
            Array.from({ length: 7 }).map((_, i) => <Skeleton key={i} className="h-14 w-16 shrink-0 rounded-2xl" />)}
          {days.map((day) => {
            const first = byDay.get(day)![0];
            const { top, bottom } = formatDayChip(new Date(first.date), timeZone);
            const active = day === selectedDay;
            return (
              <button
                key={day}
                type="button"
                role="tab"
                aria-selected={active}
                data-day={day}
                onClick={() => setChosenDay(day)}
                className={cn(
                  "flex shrink-0 flex-col items-center rounded-2xl px-3 py-1.5 transition-colors",
                  active ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground",
                  day === today && !active && "ring-1 ring-primary/40"
                )}
              >
                <span className="text-[11px] font-semibold capitalize">{top}</span>
                <span className="text-sm font-bold">{bottom}</span>
                <span className="font-mono text-[10px] opacity-80">
                  {byDay.get(day)!.length} match{byDay.get(day)!.length > 1 ? "s" : ""}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      <div className="-mt-2 flex flex-col gap-3">
        <div className="glass-scroll flex items-center gap-2 overflow-x-auto pb-0.5">
          {STATUSES.map(({ value, label }) => (
            <button key={value} type="button" className={chipClass(status === value)} onClick={() => setStatus(value)}>
              {value === "live" && <Radio className="h-3.5 w-3.5" />}
              {label}
            </button>
          ))}
          <span aria-hidden className="mx-1 h-5 w-px shrink-0 bg-tint/15" />
          <button
            type="button"
            className={chipClass(showFilters || secondaryFilterCount > 0)}
            aria-expanded={showFilters}
            onClick={() => setShowFilters((v) => !v)}
          >
            <SlidersHorizontal className="h-3.5 w-3.5" />
            Filtres{secondaryFilterCount > 0 && ` (${secondaryFilterCount})`}
          </button>
          {hasFilters && (
            <Button variant="ghost" size="sm" className="shrink-0" onClick={resetFilters}>
              <X className="h-3.5 w-3.5" />
              Réinitialiser
            </Button>
          )}
        </div>

        {showFilters && (
          <div className="glass-scroll flex items-center gap-2 overflow-x-auto pb-0.5" role="group" aria-label="Conférence">
            {CONFERENCES.map((c) => (
              <button key={c} type="button" className={chipClass(conference === c)} onClick={() => setConference(c)}>
                {c}
              </button>
            ))}
          </div>
        )}

        {showFilters && showPhases && (
          <div className="glass-scroll flex items-center gap-2 overflow-x-auto pb-0.5" role="group" aria-label="Phase">
            <button type="button" className={chipClass(activePhase === "all")} onClick={() => setPhase("all")}>
              Toutes phases
            </button>
            {phases.map((p) => {
              const Icon = MATCH_TYPE_META[p].icon;
              return (
                <button key={p} type="button" className={chipClass(activePhase === p)} onClick={() => setPhase(p)}>
                  {Icon && <Icon className="h-3.5 w-3.5" />}
                  {MATCH_TYPE_META[p].label}
                </button>
              );
            })}
          </div>
        )}

        {showFilters && sortedTeams.length > 0 && (
          <div className="glass-scroll flex items-center gap-1.5 overflow-x-auto pb-0.5" role="group" aria-label="Équipe">
            {favorite && (
              <button
                type="button"
                className={chipClass(team === favorite)}
                onClick={() => setTeam(team === favorite ? null : favorite)}
              >
                <Heart className="h-3.5 w-3.5" />
                Mon équipe
              </button>
            )}
            {sortedTeams.map((t) => (
              <button
                key={t.abbreviation}
                type="button"
                title={t.name}
                aria-label={t.name}
                aria-pressed={team === t.abbreviation}
                onClick={() => setTeam(team === t.abbreviation ? null : t.abbreviation)}
                className={cn(
                  "flex shrink-0 items-center justify-center rounded-full p-1.5 transition-all",
                  team === t.abbreviation ? "glass-accent" : team ? "opacity-40 hover:opacity-100" : "hover:bg-tint/[0.06]"
                )}
              >
                <TeamLogo abbreviation={t.abbreviation} logoUrl={t.logoUrl} size={26} />
              </button>
            ))}
          </div>
        )}
      </div>

      {isError && <p className="text-destructive">Impossible de charger les matchs. Réessaie plus tard.</p>}

      {isLoading && (
        <div className="grid gap-4 xl:grid-cols-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-64 w-full rounded-2xl" />
          ))}
        </div>
      )}

      {favoriteNext && !team && (
        <section className="flex flex-col gap-3">
          <h2 className="flex items-center gap-2 font-heading text-sm font-bold uppercase tracking-wide text-primary">
            <Heart className="h-4 w-4 fill-current" />
            Le prochain match de ton équipe
          </h2>
          <div className="grid gap-4 xl:grid-cols-2">
            <MatchCard match={favoriteNext} />
          </div>
        </section>
      )}

      {activePhase === "preseason" && (
        <div className="glass-inset-quiet flex items-start gap-3 rounded-2xl px-4 py-3 text-sm">
          <FlaskConical className="mt-0.5 h-4 w-4 shrink-0 text-muted-foreground" />
          <p className="text-muted-foreground">
            <strong className="text-foreground">Présaison</strong> : matchs de préparation où les titulaires jouent peu.
            Tu peux parier dessus, mais leurs résultats ne comptent pas dans la force (Elo) des équipes.
          </p>
        </div>
      )}

      {activePhase === "nba_cup" && (
        <div className="flex items-center gap-3 rounded-2xl bg-amber-400/10 px-4 py-3 text-sm shadow-[inset_0_0_0_1px_rgba(251,191,36,0.25)]">
          <Trophy className="h-6 w-6 shrink-0 text-amber-400 light:text-amber-800" />
          <p className="text-muted-foreground">
            <strong className="text-foreground">Coupe NBA</strong> : phase de groupes puis élimination directe jusqu&apos;à la
            finale. Seule la finale ne compte pas au classement de la saison.
          </p>
        </div>
      )}

      {activePhase === "playoffs" && <PlayoffSeries matches={scoped} />}

      {live.length > 0 && (
        <section className="flex flex-col gap-3">
          <h2 className="flex items-center gap-2 font-heading text-sm font-bold uppercase tracking-wide text-live">
            <Radio className="h-4 w-4 animate-pulse" />
            En direct
          </h2>
          <div className="stagger-children grid gap-4 xl:grid-cols-2">
            {live.map((m) => (
              <MatchCard key={m.id} match={m} />
            ))}
          </div>
        </section>
      )}

      {!isLoading && !isError && days.length === 0 && live.length === 0 && (
        <div className="glass flex flex-col items-center gap-3 rounded-2xl py-10 text-center">
          <CalendarDays className="h-8 w-8 text-muted-foreground" />
          <p className="text-muted-foreground">Aucun match pour ces filtres.</p>
          {hasFilters && (
            <Button variant="outline" size="sm" onClick={resetFilters}>
              Réinitialiser les filtres
            </Button>
          )}
        </div>
      )}

      {selectedDay && dayMatches.length > 0 && (
        <section className="flex flex-col gap-3">
          <div className="flex flex-wrap items-baseline justify-between gap-2">
            <h2 className="font-heading text-sm font-bold uppercase tracking-wide text-muted-foreground">
              {getDayLabel(new Date(dayMatches[0].date), timeZone)}
            </h2>
            {noMatchToday && (
              <p className="text-xs text-muted-foreground">Pas de match aujourd&apos;hui : voici le prochain jour de match.</p>
            )}
          </div>
          {/* key = jour : la grille rejoue son entrée en cascade à chaque changement de jour. */}
          {dayOpen.length > 0 && (
            <div key={selectedDay} className="stagger-children grid gap-4 xl:grid-cols-2">
              {dayOpen.map((m) => (
                <MatchCard key={m.id} match={m} />
              ))}
            </div>
          )}
          {dayFinished.length > 0 && (
            <div className="flex flex-col gap-2">
              {dayOpen.length > 0 && (
                <h3 className="mt-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Terminés</h3>
              )}
              <div className="grid gap-2 xl:grid-cols-2">
                {dayFinished.map((m) => (
                  <FinishedMatchRow key={m.id} match={m} />
                ))}
              </div>
            </div>
          )}
        </section>
      )}
    </div>
  );
}
