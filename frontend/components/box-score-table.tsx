"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ChevronDown } from "lucide-react";
import { TeamLogo } from "@/components/team-logo";
import { fetchTeamRoster } from "@/lib/api/teams";
import { normalizePlayerName } from "@/lib/player-props";
import type { Match, PlayerBoxScore, RosterPlayer, Team } from "@/lib/types";
import { cn } from "@/lib/utils";

type Side = "away" | "both" | "home";

const made = (split: string) => Number.parseInt(split.split("-")[0], 10) || 0;
const minutes = (m: string) => Number.parseInt(m, 10) || 0;

interface Column {
  key: string;
  label: string;
  title: string;
  value: (p: PlayerBoxScore) => number;
  display?: (p: PlayerBoxScore) => string;
}

// Ordre de lecture d'une feuille de match : temps de jeu, puis les stats
// qui comptent le plus pour les paris (points, rebonds, passes, 3 pts).
const COLUMNS: Column[] = [
  { key: "min", label: "MIN", title: "Minutes", value: (p) => minutes(p.minutes), display: (p) => (minutes(p.minutes) ? `${minutes(p.minutes)}'` : "-") },
  { key: "pts", label: "PTS", title: "Points", value: (p) => p.points },
  { key: "reb", label: "REB", title: "Rebonds", value: (p) => p.rebounds },
  { key: "pd", label: "PD", title: "Passes décisives", value: (p) => p.assists },
  { key: "3pt", label: "3PTS", title: "Tirs à 3 points réussis-tentés", value: (p) => made(p.threePoints), display: (p) => p.threePoints },
  { key: "tirs", label: "TIRS", title: "Tirs réussis-tentés", value: (p) => made(p.fieldGoals), display: (p) => p.fieldGoals },
  { key: "lf", label: "LF", title: "Lancers francs réussis-tentés", value: (p) => made(p.freeThrows), display: (p) => p.freeThrows },
  { key: "int", label: "INT", title: "Interceptions", value: (p) => p.steals },
  { key: "ctr", label: "CTR", title: "Contres", value: (p) => p.blocks },
  { key: "bp", label: "BP", title: "Balles perdues", value: (p) => p.turnovers },
  { key: "pm", label: "+/-", title: "Écart au score quand il était sur le terrain", value: (p) => p.plusMinus, display: (p) => (p.plusMinus > 0 ? `+${p.plusMinus}` : String(p.plusMinus)) },
];

/** Photo, numéro et poste : la feuille de match n'a que le nom, on les reprend de l'effectif (même règle de nom qu'au règlement). */
function useRosterIndex(teams: Team[]) {
  const away = useQuery({ queryKey: ["team-roster", teams[0].id], queryFn: () => fetchTeamRoster(teams[0].id), staleTime: 60 * 60 * 1000 });
  const home = useQuery({ queryKey: ["team-roster", teams[1].id], queryFn: () => fetchTeamRoster(teams[1].id), staleTime: 60 * 60 * 1000 });
  return useMemo(() => {
    const index = new Map<string, RosterPlayer>();
    [...(away.data ?? []), ...(home.data ?? [])].forEach((p) =>
      index.set(normalizePlayerName(`${p.firstName} ${p.lastName}`), p)
    );
    return index;
  }, [away.data, home.data]);
}

function shortName(name: string) {
  const [first, ...rest] = name.split(" ");
  return rest.length ? `${first[0]}. ${rest.join(" ")}` : name;
}

/**
 * Feuille de match façon appli de scores : une équipe ou les deux, triable
 * par colonne, nom du joueur figé pendant qu'on fait défiler les stats.
 * Titulaires marqués d'un liseré.
 */
export function BoxScoreTable({ match, players }: { match: Match; players: PlayerBoxScore[] }) {
  const [side, setSide] = useState<Side>("both");
  const [sortKey, setSortKey] = useState("pts");
  const roster = useRosterIndex([match.awayTeam, match.homeTeam]);

  const column = COLUMNS.find((c) => c.key === sortKey) ?? COLUMNS[1];
  const teamOf = (p: PlayerBoxScore) => (p.teamAbbreviation === match.homeTeam.abbreviation ? match.homeTeam : match.awayTeam);
  const shown = players
    .filter((p) => side === "both" || teamOf(p) === (side === "home" ? match.homeTeam : match.awayTeam))
    .sort((a, b) => column.value(b) - column.value(a) || b.points - a.points);

  const sides: { value: Side; content: React.ReactNode; label: string }[] = [
    { value: "away", label: match.awayTeam.name, content: <TeamLogo abbreviation={match.awayTeam.abbreviation} logoUrl={match.awayTeam.logoUrl} size={22} /> },
    {
      value: "both",
      label: "Les deux équipes",
      content: (
        <span className="flex items-center gap-1">
          <TeamLogo abbreviation={match.awayTeam.abbreviation} logoUrl={match.awayTeam.logoUrl} size={20} />
          <span className="text-xs text-muted-foreground">+</span>
          <TeamLogo abbreviation={match.homeTeam.abbreviation} logoUrl={match.homeTeam.logoUrl} size={20} />
        </span>
      ),
    },
    { value: "home", label: match.homeTeam.name, content: <TeamLogo abbreviation={match.homeTeam.abbreviation} logoUrl={match.homeTeam.logoUrl} size={22} /> },
  ];

  return (
    <div className="flex flex-col gap-3">
      <div className="glass-inset-quiet grid grid-cols-3 gap-1 rounded-full p-1" role="tablist" aria-label="Équipe">
        {sides.map((s) => (
          <button
            key={s.value}
            type="button"
            role="tab"
            aria-selected={side === s.value}
            aria-label={s.label}
            onClick={() => setSide(s.value)}
            className={cn(
              "flex min-h-10 items-center justify-center rounded-full transition-colors",
              side === s.value ? "glass-accent" : "opacity-70 hover:opacity-100"
            )}
          >
            {s.content}
          </button>
        ))}
      </div>

      {/* Fond opaque (bg-card) et non du verre : la colonne des noms, figée,
          doit masquer les chiffres qui défilent dessous. */}
      <div className="overflow-hidden rounded-2xl bg-card">
        <div className="glass-scroll overflow-x-auto">
          <table className="w-full border-separate border-spacing-0 text-sm tabular-nums">
            <thead>
              <tr className="text-[11px] text-muted-foreground">
                <th className="sticky left-0 z-10 bg-card px-3 py-2.5 text-left font-medium">Joueur</th>
                {COLUMNS.map((c) => (
                  <th key={c.key} className="px-2 py-2.5 text-center font-medium">
                    <button
                      type="button"
                      title={`Trier par ${c.title.toLowerCase()}`}
                      onClick={() => setSortKey(c.key)}
                      className={cn(
                        "inline-flex items-center gap-0.5 whitespace-nowrap",
                        c.key === sortKey ? "font-bold text-primary" : "hover:text-foreground"
                      )}
                    >
                      {c.label}
                      {c.key === sortKey && <ChevronDown className="h-3 w-3" />}
                    </button>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {shown.map((p) => {
                const info = roster.get(normalizePlayerName(p.playerName));
                const team = teamOf(p);
                return (
                  <tr key={`${p.teamAbbreviation}-${p.playerName}`}>
                    <td
                      className={cn(
                        "sticky left-0 z-10 border-t border-tint/10 bg-card py-2 pl-3 pr-2",
                        p.starter && "shadow-[inset_3px_0_0_var(--brand)]"
                      )}
                    >
                      <div className="flex w-36 items-center gap-2 sm:w-44">
                        <span className="relative shrink-0">
                          {info?.headshotUrl ? (
                            // eslint-disable-next-line @next/next/no-img-element
                            <img src={info.headshotUrl} alt="" className="h-8 w-8 rounded-full bg-tint/10 object-cover" />
                          ) : (
                            <span className="block h-8 w-8 rounded-full bg-tint/10" />
                          )}
                          {side === "both" && (
                            <span className="absolute -bottom-1 -right-1">
                              <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={14} />
                            </span>
                          )}
                        </span>
                        <span className="min-w-0">
                          <span className="block truncate font-medium">{shortName(p.playerName)}</span>
                          <span className="block truncate text-[11px] text-muted-foreground">
                            {[info?.jersey, info?.position].filter(Boolean).join(" ") || team.abbreviation}
                          </span>
                        </span>
                      </div>
                    </td>
                    {COLUMNS.map((c) => (
                      <td
                        key={c.key}
                        className={cn(
                          "border-t border-tint/10 px-2 py-2 text-center font-mono text-xs",
                          c.key === sortKey ? "font-bold text-primary" : "text-foreground/80"
                        )}
                      >
                        {c.display ? c.display(p) : c.value(p)}
                      </td>
                    ))}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
      <p className="text-[11px] text-muted-foreground">
        <span className="mr-1 inline-block h-2.5 w-0.5 bg-brand align-middle" /> titulaire · touche une colonne pour trier
      </p>
    </div>
  );
}
