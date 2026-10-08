"use client";

import { useQuery } from "@tanstack/react-query";
import { Card, CardContent } from "@/components/ui/card";
import { OddsButton } from "@/components/odds-button";
import { fetchPlayerProps } from "@/lib/api/matches";
import type { BetSelection, Match, PlayerProp } from "@/lib/types";

function shortName(name: string) {
  const [first, ...rest] = name.split(" ");
  return rest.length ? `${first[0]}. ${rest.join(" ")}` : name;
}

function selection(match: Match, prop: PlayerProp, outcome: "over" | "under"): BetSelection {
  const line = prop.line.toString().replace(".", ",");
  return {
    id: `${match.id}-player_points-${prop.playerId}-${outcome}`,
    matchId: match.id,
    matchLabel: `${match.awayTeam.name} vs ${match.homeTeam.name}`,
    market: "player_points",
    outcome,
    // Même libellé que celui construit par le serveur (BetController.propLabel).
    label: `${shortName(prop.playerName)} · ${outcome === "over" ? "Plus" : "Moins"} de ${line} pts`,
    odds: outcome === "over" ? prop.overOdds : prop.underOdds,
    playerId: prop.playerId,
    line: prop.line,
  };
}

/**
 * Paris joueurs : plus/moins de X points pour les meilleurs marqueurs de
 * chaque équipe. Rien n'est affiché s'il n'y en a pas (présaison, moyennes
 * pas encore synchronisées).
 */
export function PlayerPropsCard({ match }: { match: Match }) {
  const { data, isError } = useQuery({
    queryKey: ["player-props", match.id],
    queryFn: () => fetchPlayerProps(match.id),
    staleTime: 5 * 60 * 1000,
  });
  if (isError || !data || data.length === 0) return null;

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-6">
        <div>
          <h2 className="font-heading text-base font-bold">Paris joueurs</h2>
          <p className="text-xs text-muted-foreground">
            Plus ou moins de points que sa moyenne de la saison. Joueur absent : sélection remboursée. Compte comme la
            sélection de ce match dans ton ticket.
          </p>
        </div>
        <div className="flex flex-col divide-y divide-tint/10">
          {data.map((prop) => (
            <div key={prop.playerId} className="flex items-center gap-3 py-2.5">
              {prop.headshotUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={prop.headshotUrl} alt="" className="h-10 w-10 shrink-0 rounded-full bg-tint/10 object-cover" />
              ) : (
                <span className="h-10 w-10 shrink-0 rounded-full bg-tint/10" aria-hidden />
              )}
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold">{shortName(prop.playerName)}</p>
                <p className="font-mono text-[11px] text-muted-foreground">
                  {prop.teamAbbreviation} · {prop.pointsPerGame.toFixed(1).replace(".", ",")} pts/match
                </p>
              </div>
              <div className="flex w-40 shrink-0 gap-1.5">
                <OddsButton selection={selection(match, prop, "over")} label={`+ de ${prop.line.toString().replace(".", ",")}`} />
                <OddsButton selection={selection(match, prop, "under")} label={`− de ${prop.line.toString().replace(".", ",")}`} />
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
