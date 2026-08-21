"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { MatchCard } from "@/components/match-card";
import { fetchMatches } from "@/lib/api/matches";
import { Conference, Match } from "@/lib/types";
import { cn, getDayLabel } from "@/lib/utils";

const CONFERENCE_FILTERS: (Conference | "Toutes")[] = ["Toutes", "Est", "Ouest"];

export default function MatchesPage() {
  const [conferenceFilter, setConferenceFilter] = useState<Conference | "Toutes">("Toutes");

  const { data, isLoading, isError } = useQuery({
    queryKey: ["matches"],
    queryFn: fetchMatches,
    refetchInterval: 60 * 1000, // les cotes bougent en continu côté serveur (cf. OddsService) — on suit
  });

  const filteredMatches = useMemo(() => {
    if (!data) return [];
    const base =
      conferenceFilter === "Toutes"
        ? data
        : data.filter(
            (m) =>
              m.homeTeam.conference === conferenceFilter ||
              m.awayTeam.conference === conferenceFilter
          );
    return [...base].sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
  }, [data, conferenceFilter]);

  const groupedByDay = useMemo(() => {
    const groups: { label: string; matches: Match[] }[] = [];
    for (const match of filteredMatches) {
      const label = getDayLabel(new Date(match.date));
      const lastGroup = groups[groups.length - 1];
      if (lastGroup && lastGroup.label === label) {
        lastGroup.matches.push(match);
      } else {
        groups.push({ label, matches: [match] });
      }
    }
    return groups;
  }, [filteredMatches]);

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-bold">Matchs</h1>
        <p className="mt-1 text-muted-foreground">
          Calendrier de la saison régulière NBA.
        </p>
      </div>

      <div className="flex gap-2">
        {CONFERENCE_FILTERS.map((conf) => (
          <button
            key={conf}
            type="button"
            onClick={() => setConferenceFilter(conf)}
          >
            <Badge
              variant="outline"
              className={cn(
                "cursor-pointer px-3 py-1.5 transition-colors",
                conferenceFilter === conf
                  ? "border-primary/40 bg-primary/10 text-primary"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              {conf}
            </Badge>
          </button>
        ))}
      </div>

      <div className="flex flex-col gap-3">
        {isLoading &&
          Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-20 w-full rounded-lg" />
          ))}

        {isError && (
          <p className="text-destructive">
            Impossible de charger les matchs. Réessaie plus tard.
          </p>
        )}

        {!isLoading && !isError && filteredMatches.length === 0 && (
          <p className="text-muted-foreground">
            Aucun match pour ce filtre.
          </p>
        )}

        {!isLoading &&
          !isError &&
          groupedByDay.map((group) => (
            <div key={group.label} className="flex flex-col gap-3">
              <h2 className="font-heading text-sm font-bold uppercase tracking-wide text-muted-foreground">
                {group.label}
              </h2>
              {group.matches.map((match) => (
                <MatchCard key={match.id} match={match} />
              ))}
            </div>
          ))}
      </div>
    </div>
  );
}