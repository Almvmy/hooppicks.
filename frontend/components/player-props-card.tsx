"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Card, CardContent } from "@/components/ui/card";
import { OddsButton } from "@/components/odds-button";
import { fetchPlayerProps } from "@/lib/api/matches";
import { PLAYER_PROP_MARKETS } from "@/lib/player-props";
import { cn } from "@/lib/utils";
import type { BetSelection, Match, PlayerProp, PlayerPropMarket } from "@/lib/types";

function shortName(name: string) {
  const [first, ...rest] = name.split(" ");
  return rest.length ? `${first[0]}. ${rest.join(" ")}` : name;
}

const decimal = (value: number, digits?: number) =>
  (digits === undefined ? value.toString() : value.toFixed(digits)).replace(".", ",");

function selection(match: Match, prop: PlayerProp, outcome: "over" | "under"): BetSelection {
  const { unit } = PLAYER_PROP_MARKETS[prop.market];
  return {
    id: `${match.id}-${prop.market}-${prop.playerId}-${outcome}`,
    matchId: match.id,
    matchLabel: `${match.awayTeam.name} vs ${match.homeTeam.name}`,
    market: prop.market,
    outcome,
    // Même libellé que celui construit par le serveur (BetController.propLabel).
    label: `${shortName(prop.playerName)} · ${outcome === "over" ? "Plus" : "Moins"} de ${decimal(prop.line)} ${unit}`,
    odds: outcome === "over" ? prop.overOdds : prop.underOdds,
    playerId: prop.playerId,
    line: prop.line,
  };
}

/**
 * Paris joueurs : plus/moins que la moyenne de la saison, en points, rebonds,
 * passes ou les trois cumulés. Rien n'est affiché s'il n'y en a pas
 * (présaison, moyennes pas encore synchronisées).
 */
export function PlayerPropsCard({ match }: { match: Match }) {
  const { data, isError } = useQuery({
    queryKey: ["player-props", match.id],
    queryFn: () => fetchPlayerProps(match.id),
    staleTime: 5 * 60 * 1000,
  });
  const [picked, setPicked] = useState<PlayerPropMarket>("player_points");

  if (isError || !data || data.length === 0) return null;

  // Seulement les statistiques qui ont au moins une ligne sur ce match.
  const markets = (Object.keys(PLAYER_PROP_MARKETS) as PlayerPropMarket[]).filter((m) =>
    data.some((p) => p.market === m)
  );
  const market = markets.includes(picked) ? picked : markets[0];

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-6">
        <div>
          <h2 className="font-heading text-base font-bold">Paris joueurs</h2>
          <p className="text-xs text-muted-foreground">
            Plus ou moins que sa moyenne de la saison. Joueur absent : sélection remboursée. Compte comme la sélection
            de ce match dans ton ticket.
          </p>
        </div>
        {markets.length > 1 && (
          // Largeur selon le libellé : « Pts+Rbd+Pd » ne tient pas dans un quart de carte sur téléphone.
          <div className="glass-inset-quiet flex gap-1 rounded-xl p-1" role="tablist" aria-label="Statistique">
            {markets.map((m) => (
              <button
                key={m}
                type="button"
                role="tab"
                aria-selected={m === market}
                onClick={() => setPicked(m)}
                className={cn(
                  "min-h-9 flex-auto whitespace-nowrap rounded-lg px-1.5 text-xs font-semibold transition-colors",
                  m === market ? "glass-accent" : "text-muted-foreground hover:text-foreground"
                )}
              >
                {PLAYER_PROP_MARKETS[m].tab}
              </button>
            ))}
          </div>
        )}
        <div className="flex flex-col divide-y divide-tint/10" role="tabpanel">
          {data
            .filter((prop) => prop.market === market)
            .map((prop) => (
              <div key={`${prop.market}-${prop.playerId}`} className="flex items-center gap-3 py-2.5">
                {prop.headshotUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={prop.headshotUrl} alt="" className="h-10 w-10 shrink-0 rounded-full bg-tint/10 object-cover" />
                ) : (
                  <span className="h-10 w-10 shrink-0 rounded-full bg-tint/10" aria-hidden />
                )}
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold">{shortName(prop.playerName)}</p>
                  <p className="truncate text-[11px] tabular-nums text-muted-foreground">
                    {prop.teamAbbreviation} · moy. {decimal(prop.average, 1)}
                  </p>
                </div>
                <div className="flex w-[8.5rem] shrink-0 gap-1">
                  <OddsButton
                    selection={selection(match, prop, "over")}
                    label={`+ de ${decimal(prop.line)}`}
                  />
                  <OddsButton
                    selection={selection(match, prop, "under")}
                    label={`− de ${decimal(prop.line)}`}
                  />
                </div>
              </div>
            ))}
        </div>
      </CardContent>
    </Card>
  );
}
