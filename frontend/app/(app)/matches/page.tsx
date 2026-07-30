"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { MatchCard } from "@/components/match-card";
import { fetchMatches } from "@/lib/api/matches";
import { Conference } from "@/lib/types";
import { cn } from "@/lib/utils";

const CONFERENCE_FILTERS: (Conference | "Toutes")[] = ["Toutes", "Est", "Ouest"];

export default function MatchesPage() {
  const [conferenceFilter, setConferenceFilter] = useState<Conference | "Toutes">("Toutes");

  const { data, isLoading, isError } = useQuery({
    queryKey: ["matches"],
    queryFn: fetchMatches,
  });

  const filteredMatches = useMemo(() => {
    if (!data) return [];
    if (conferenceFilter === "Toutes") return data;
    return data.filter(
      (m) =>
        m.homeTeam.conference === conferenceFilter ||
        m.awayTeam.conference === conferenceFilter
    );
  }, [data, conferenceFilter]);

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
          filteredMatches.map((match) => <MatchCard key={match.id} match={match} />)}
      </div>
    </div>
  );
}