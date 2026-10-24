"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Card, CardContent } from "@/components/ui/card";
import { OddsButton } from "@/components/odds-button";
import { PropMilestones } from "@/components/prop-milestones";
import { fetchPlayerProps } from "@/lib/api/matches";
import { PLAYER_PROP_MARKETS, decimal, propSelection as selection, shortPlayerName as shortName } from "@/lib/player-props";
import { cn } from "@/lib/utils";
import type { Match, PlayerPropMarket } from "@/lib/types";

/**
 * Paris joueurs : plus/moins que la moyenne de la saison (mêlée à la saison
 * passée sur les 10 premiers matchs, cf. PlayerPropsService), en points, rebonds,
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
        <div className="flex items-center justify-between gap-2">
          <h2 className="font-heading text-base font-bold">Paris joueurs</h2>
          <Link href="/paris-joueurs" className="text-xs font-semibold text-primary hover:underline">
            Tous ceux de la soirée
          </Link>
        </div>
        {markets.length > 1 && (
          // Largeur selon le libellé, et défilement horizontal : les cinq
          // statistiques ne tiennent pas toutes sur une ligne de téléphone.
          <div
            className="glass-inset-quiet glass-scroll flex gap-1 overflow-x-auto rounded-xl p-1"
            role="tablist"
            aria-label="Statistique"
          >
            {markets.map((m) => (
              <button
                key={m}
                type="button"
                role="tab"
                aria-selected={m === market}
                onClick={() => setPicked(m)}
                className={cn(
                  "min-h-9 flex-auto shrink-0 whitespace-nowrap rounded-lg px-2.5 text-xs font-semibold transition-colors",
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
              <div key={`${prop.market}-${prop.playerId}`} className="flex flex-col gap-1.5 py-2.5">
                <div className="flex items-center gap-3">
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
                      communityPct={prop.overPct}
                    />
                    <OddsButton
                      selection={selection(match, prop, "under")}
                      label={`− de ${decimal(prop.line)}`}
                      communityPct={prop.overPct === null ? null : 100 - prop.overPct}
                    />
                  </div>
                </div>
                <PropMilestones match={match} prop={prop} className="ml-[3.25rem] flex gap-1" />
              </div>
            ))}
        </div>
      </CardContent>
    </Card>
  );
}
