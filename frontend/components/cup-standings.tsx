"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Star, Trophy, X } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { TeamLogo } from "@/components/team-logo";
import { fetchCupStandings, type CupGroup, type CupTeamRow } from "@/lib/api/cup";
import { useFavoriteTeam } from "@/lib/use-teams";
import { cn } from "@/lib/utils";

const STATUS = {
  qualified: { icon: Trophy, label: "Qualifié", className: "text-primary" },
  wildcard: { icon: Star, label: "Repêché", className: "text-primary" },
  eliminated: { icon: X, label: "Éliminé", className: "text-muted-foreground" },
} as const;

function signed(n: number): string {
  return n > 0 ? `+${n}` : String(n);
}

function Row({ team }: { team: CupTeamRow }) {
  const status = team.status ? STATUS[team.status] : null;
  const name = (
    <span className="flex min-w-0 items-center gap-2 font-medium">
      <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={24} />
      <span className="truncate">{team.name}</span>
    </span>
  );
  return (
    <tr className={cn("transition-colors hover:bg-tint/[0.04]", team.status === "eliminated" && "opacity-55")}>
      <td className="max-w-0 py-2 pl-4">{team.teamId ? <Link href={`/teams/${team.teamId}`} className="hover:underline">{name}</Link> : name}</td>
      <td className="px-2 py-2 text-right font-mono">{team.wins}</td>
      <td className="px-2 py-2 text-right font-mono">{team.losses}</td>
      <td className="px-2 py-2 text-right font-mono">{signed(team.diff)}</td>
      <td className="px-2 py-2 text-right font-mono text-muted-foreground">{team.pointsFor}</td>
      <td className="w-7 py-2 pr-3 text-right">
        {status && <status.icon className={cn("ml-auto h-3.5 w-3.5", status.className)} aria-label={status.label} />}
      </td>
    </tr>
  );
}

function GroupTable({ group }: { group: CupGroup }) {
  return (
    <Card className="overflow-hidden">
      <CardContent className="px-0 pt-4">
        <p className="mb-2 px-4 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          {group.conference} · Poule {group.name}
        </p>
        <table className="w-full table-fixed text-sm">
          <thead>
            <tr className="text-left text-[11px] uppercase text-muted-foreground">
              <th className="py-1 pl-4 font-medium">Équipe</th>
              <th className="w-9 px-2 py-1 text-right font-medium">V</th>
              <th className="w-9 px-2 py-1 text-right font-medium">D</th>
              <th className="w-12 px-2 py-1 text-right font-medium">Diff</th>
              <th className="w-12 px-2 py-1 text-right font-medium">Pts</th>
              <th className="w-7 py-1 pr-3" aria-label="État" />
            </tr>
          </thead>
          <tbody>
            {group.teams.map((team) => (
              <Row key={team.abbreviation} team={team} />
            ))}
          </tbody>
        </table>
      </CardContent>
    </Card>
  );
}

/** Le classement des poules de la Coupe NBA : celui des équipes, pas celui des pronostiqueurs. */
export function CupStandings() {
  const { team: favorite } = useFavoriteTeam();
  const [chosen, setChosen] = useState<"Est" | "Ouest" | null>(null);
  const conference = chosen ?? (favorite?.conference === "Ouest" ? "Ouest" : "Est");
  const { data, isLoading, isError } = useQuery({ queryKey: ["cup", "standings"], queryFn: fetchCupStandings, staleTime: 60_000 });

  if (isLoading) return <Skeleton className="h-96 w-full rounded-2xl" />;
  if (isError) return <p className="text-destructive">Impossible de charger les poules.</p>;
  if (!data || data.groups.length === 0) return <p className="text-sm text-muted-foreground">Pas de poules pour l&apos;instant.</p>;

  return (
    <div className="flex flex-col gap-4">
      <div className="glass-inset-quiet grid grid-cols-2 gap-1 rounded-full p-1 xl:hidden" role="tablist" aria-label="Conférence">
        {(["Est", "Ouest"] as const).map((conf) => (
          <button
            key={conf}
            type="button"
            role="tab"
            aria-selected={conference === conf}
            onClick={() => setChosen(conf)}
            className={cn("min-h-9 rounded-full text-sm font-semibold transition-colors", conference === conf ? "glass-accent" : "text-muted-foreground hover:text-foreground")}
          >
            Conférence {conf}
          </button>
        ))}
      </div>
      <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
        {(["Est", "Ouest"] as const).map((conf) => (
          <div key={conf} className={cn("flex flex-col gap-4", conf !== conference && "hidden xl:flex")}>
            {data.groups.filter((g) => g.conference === conf).map((group) => (
              <GroupTable key={group.name} group={group} />
            ))}
          </div>
        ))}
      </div>
      <p className="flex flex-wrap gap-x-4 gap-y-1 text-[11px] text-muted-foreground">
        <span className="flex items-center gap-1"><Trophy className="h-3 w-3 text-primary" /> premier de poule</span>
        <span className="flex items-center gap-1"><Star className="h-3 w-3 text-primary" /> meilleur deuxième</span>
        <span className="flex items-center gap-1"><X className="h-3 w-3" /> éliminé</span>
        <span>Connus une fois les poules finies</span>
      </p>
    </div>
  );
}
