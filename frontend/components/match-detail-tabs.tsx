"use client";

import { useRouter } from "next/navigation";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { MatchOddsRow } from "@/components/match-odds-row";
import { PlayerPropsCard } from "@/components/player-props-card";
import { TeamRoster } from "@/components/team-roster";
import { MatchBoxScore } from "@/components/match-box-score";
import { TeamNews } from "@/components/news/team-news";
import { BoxScoreSection, LeadersSection, LinescoreSection, TeamStatsSection } from "@/components/match-live-sections";
import { useLiveMatch } from "@/lib/live";
import type { Match } from "@/lib/types";
import { bettingClosedReason, cn, isBettable } from "@/lib/utils";

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
    router.replace(next === tabs[0] ? `/matches/${match.id}` : `/matches/${match.id}?onglet=${next}`, { scroll: false });
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="glass-scroll -mx-1 flex gap-2 overflow-x-auto px-1 pb-0.5" role="tablist" aria-label="Rubriques du match">
        {tabs.map((t) => (
          <button
            key={t}
            type="button"
            role="tab"
            aria-selected={t === active}
            onClick={() => select(t)}
            className={cn(
              "flex min-h-9 shrink-0 items-center rounded-full px-4 text-sm font-semibold transition-colors",
              t === active ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
            )}
          >
            {LABELS[t]}
          </button>
        ))}
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

          {active === "feuille" &&
            (live && live.players.length > 0 ? (
              <BoxScoreSection match={match} players={live.players} />
            ) : (
              <Card>
                <CardContent className="pt-6">
                  <MatchBoxScore matchId={match.id} homeTeam={match.homeTeam} awayTeam={match.awayTeam} />
                </CardContent>
              </Card>
            ))}

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
