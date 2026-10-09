"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { MatchOddsRow } from "@/components/match-odds-row";
import { PlayerPropsCard } from "@/components/player-props-card";
import { TeamRoster } from "@/components/team-roster";
import { useQuery } from "@tanstack/react-query";
import { BoxScoreTable } from "@/components/box-score-table";
import { fetchMatchBoxScore } from "@/lib/api/matches";
import { TeamNews } from "@/components/news/team-news";
import { LeadersSection, LinescoreSection, TeamStatsSection } from "@/components/match-live-sections";
import { TeamLogo } from "@/components/team-logo";
import { liveClockLabel, useLiveMatch, useLiveStatus } from "@/lib/live";
import type { Match, PlayerBoxScore } from "@/lib/types";
import { bettingClosedReason, cn, isBettable } from "@/lib/utils";

/** Feuille de match du direct ESPN, sinon celle importée en base après le match. */
function BoxScoreTab({ match, livePlayers }: { match: Match; livePlayers: PlayerBoxScore[] | undefined }) {
  const fromEspn = !!livePlayers && livePlayers.length > 0;
  const { data, isLoading, isError } = useQuery({
    queryKey: ["match-boxscore", match.id],
    queryFn: () => fetchMatchBoxScore(match.id),
    enabled: !fromEspn,
  });
  if (fromEspn) return <BoxScoreTable match={match} players={livePlayers} />;
  if (isLoading) return <Skeleton className="h-64 w-full rounded-2xl" />;
  if (isError) return <p className="text-sm text-destructive">Impossible de charger la feuille de match.</p>;
  if (!data || data.length === 0) {
    return <p className="text-sm text-muted-foreground">Feuille de match pas encore disponible pour ce match.</p>;
  }
  return <BoxScoreTable match={match} players={data} />;
}

/** Score en petit, collé en haut avec les onglets : on garde l'œil dessus en parcourant la feuille de match. */
function StickyScore({ match }: { match: Match }) {
  const live = useLiveStatus(match);
  return (
    <div className="flex items-center justify-center gap-2.5 pb-2">
      <TeamLogo abbreviation={match.awayTeam.abbreviation} logoUrl={match.awayTeam.logoUrl} size={24} />
      <span className="font-heading text-sm font-bold">{match.awayTeam.abbreviation}</span>
      <span className="font-mono text-lg font-bold tabular-nums">
        {match.awayScore ?? 0} - {match.homeScore ?? 0}
      </span>
      <span className="font-heading text-sm font-bold">{match.homeTeam.abbreviation}</span>
      <TeamLogo abbreviation={match.homeTeam.abbreviation} logoUrl={match.homeTeam.logoUrl} size={24} />
      <span
        className={cn(
          "ml-1 whitespace-nowrap font-mono text-[11px] font-semibold",
          match.status === "live" ? "text-live" : "text-muted-foreground",
        )}
      >
        {match.status === "finished" ? "Terminé" : live ? liveClockLabel(live) : "En direct"}
      </span>
    </div>
  );
}

type Tab = "paris" | "resume" | "stats" | "feuille" | "effectifs" | "actus";

const LABELS: Record<Tab, string> = {
  paris: "Paris",
  resume: "Résumé",
  stats: "Stats",
  feuille: "Feuille de match",
  effectifs: "Effectifs",
  actus: "Actus",
};

/**
 * Le contenu d'un match rangé en onglets, sous l'en-tête (score). Avant le
 * coup d'envoi : paris, effectifs, actus. Pendant et après : résumé
 * (quarts-temps, meilleurs joueurs), stats, feuille de match, actus.
 *
 * Résumé et stats viennent d'ESPN (direct, puis résumé du match terminé) :
 * sans ESPN, ces onglets disparaissent, la feuille de match retombe sur celle
 * importée en base après le match, et pendant le match on revient aux
 * effectifs. L'onglet ouvert est dans l'adresse (?onglet=stats).
 */
export function MatchDetailTabs({ match, tab: requested }: { match: Match; tab: string | undefined }) {
  const router = useRouter();
  const { data: live, isLoading } = useLiveMatch(match);
  // Onglets collés sous la barre du haut (h-16) dès que le grand en-tête du
  // match est sorti de l'écran : on y ajoute alors le score en petit.
  const sentinel = useRef<HTMLDivElement>(null);
  const [stuck, setStuck] = useState(false);
  useEffect(() => {
    const el = sentinel.current;
    if (!el) return;
    const observer = new IntersectionObserver(([entry]) => setStuck(!entry.isIntersecting), {
      rootMargin: "-64px 0px 0px 0px",
    });
    observer.observe(el);
    return () => observer.disconnect();
  }, []);
  const started = match.status !== "scheduled";

  const tabs: Tab[] = !started
    ? ["paris", "effectifs", "actus"]
    : live
      ? ["resume", ...(live.teamStats.length > 0 ? (["stats"] as Tab[]) : []), "feuille", "actus"]
      : match.status === "finished"
        ? ["feuille", "actus"]
        : ["effectifs", "actus"];
  const active: Tab = tabs.includes(requested as Tab) ? (requested as Tab) : tabs[0];

  function select(next: Tab) {
    router.replace(next === tabs[0] ? `/matches/${match.id}` : `/matches/${match.id}?onglet=${next}`, {
      scroll: false,
    });
  }

  return (
    <div className="flex flex-col gap-4">
      <div ref={sentinel} aria-hidden className="-mb-4 h-0" />
      {/* -mx-6/px-6 : le bandeau collé couvre toute la largeur du <main> (p-6). */}
      <div className={cn("sticky top-16 z-20 -mx-6 px-6 pt-2", stuck && "glass-chrome pb-2")}>
        {stuck && started && <StickyScore match={match} />}
        <div
          className="glass-scroll -mx-1 flex gap-2 overflow-x-auto px-1 pb-0.5"
          role="tablist"
          aria-label="Rubriques du match"
        >
          {tabs.map((t) => (
            <button
              key={t}
              type="button"
              role="tab"
              aria-selected={t === active}
              onClick={() => select(t)}
              className={cn(
                "flex min-h-9 shrink-0 items-center rounded-full px-4 text-sm font-semibold transition-colors",
                t === active ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground",
              )}
            >
              {LABELS[t]}
            </button>
          ))}
        </div>
      </div>

      {/* Le premier chargement du résumé ESPN décide des onglets : un
          squelette plutôt que des onglets qui sautent une seconde après. */}
      {started && isLoading ? (
        <Skeleton className="h-48 w-full rounded-2xl" />
      ) : (
        <div role="tabpanel" className="flex flex-col gap-4">
          {active === "paris" &&
            (isBettable(match) ? (
              <>
                <Card>
                  <CardContent className="pt-6">
                    <h2 className="font-heading text-base font-bold">Parier sur ce match</h2>
                    <p className="text-xs text-muted-foreground">
                      Vainqueur, écart ou total : une sélection par match et par ticket.
                    </p>
                    <MatchOddsRow match={match} />
                  </CardContent>
                </Card>
                <PlayerPropsCard match={match} />
              </>
            ) : (
              <p className="glass-inset-quiet rounded-xl px-3 py-2 text-center text-sm text-muted-foreground first-letter:uppercase">
                {bettingClosedReason(match)}.
              </p>
            ))}

          {active === "resume" && live && (
            <>
              <LinescoreSection match={match} live={live} />
              <LeadersSection match={match} players={live.players} />
            </>
          )}

          {active === "stats" && live && <TeamStatsSection match={match} live={live} />}

          {active === "feuille" && <BoxScoreTab match={match} livePlayers={live?.players} />}

          {active === "effectifs" && (
            <Card>
              <CardContent className="grid gap-6 pt-6 sm:grid-cols-2">
                <TeamRoster team={match.awayTeam} matchday />
                <TeamRoster team={match.homeTeam} matchday />
              </CardContent>
            </Card>
          )}

          {active === "actus" && (
            <TeamNews abbreviations={[match.awayTeam.abbreviation, match.homeTeam.abbreviation]} />
          )}
        </div>
      )}
    </div>
  );
}
