"use client";

import { useQuery } from "@tanstack/react-query";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { TeamLogo } from "@/components/team-logo";
import { apiFetch } from "@/lib/api/http";
import type { Match, Team } from "@/lib/types";
import { cn, formatMatchDate } from "@/lib/utils";

interface FormGame {
  date: string | null;
  opponent: string;
  home: boolean;
  teamScore: number;
  opponentScore: number;
  won: boolean;
}

interface H2HGame {
  date: string;
  homeAbbreviation: string;
  awayAbbreviation: string;
  homeScore: number;
  awayScore: number;
  playoffs: boolean;
}

interface Preview {
  awayForm: FormGame[];
  homeForm: FormGame[];
  headToHead: H2HGame[];
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return <h2 className="font-heading text-sm font-bold uppercase tracking-wide text-muted-foreground">{children}</h2>;
}

function FormRow({ team, games }: { team: Team; games: FormGame[] }) {
  // Du plus ancien au plus récent, comme sur les applis de scores : le dernier match à droite.
  const ordered = [...games].sort((a, b) => (a.date ?? "").localeCompare(b.date ?? ""));
  return (
    <div className="flex items-center gap-3">
      <span className="flex w-16 shrink-0 items-center gap-1.5 text-sm font-semibold">
        <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={20} />
        {team.abbreviation}
      </span>
      {ordered.length === 0 ? (
        <span className="text-xs text-muted-foreground">Pas de match récent.</span>
      ) : (
        <span className="flex gap-1.5">
          {ordered.map((g, i) => (
            <span
              key={i}
              title={`${g.home ? "contre" : "à"} ${g.opponent} · ${g.teamScore}-${g.opponentScore}${g.date ? ` · ${formatMatchDate(new Date(g.date))}` : ""}`}
              className={cn(
                "flex h-7 w-7 items-center justify-center rounded-md text-xs font-bold text-white",
                g.won ? "bg-success" : "bg-destructive"
              )}
            >
              {g.won ? "G" : "P"}
            </span>
          ))}
        </span>
      )}
    </div>
  );
}

/**
 * L'avant-match : forme des deux équipes et dernières confrontations (ESPN
 * et notre base, MatchPreviewService côté serveur). Rien d'inventé : sans
 * donnée, on le dit.
 */
export function MatchPreviewSection({ match }: { match: Match }) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["match-preview", match.id],
    queryFn: () => apiFetch<Preview>(`/matches/${match.id}/preview`),
    staleTime: 30 * 60 * 1000,
  });

  if (isLoading) return <Skeleton className="h-56 w-full rounded-2xl" />;
  if (isError || !data) return <p className="text-sm text-destructive">Impossible de charger l&apos;avant-match.</p>;

  const teams = [match.awayTeam, match.homeTeam];
  const wins = (abbr: string) =>
    data.headToHead.filter((g) => (g.homeScore > g.awayScore ? g.homeAbbreviation : g.awayAbbreviation) === abbr).length;

  return (
    <>
      <Card>
        <CardContent className="flex flex-col gap-3 pt-6">
          <SectionTitle>Forme (5 derniers matchs)</SectionTitle>
          <FormRow team={match.awayTeam} games={data.awayForm} />
          <FormRow team={match.homeTeam} games={data.homeForm} />
          <p className="text-[11px] text-muted-foreground">
            G : gagné · P : perdu · le plus récent à droite. Présaison et playoffs compris.
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="flex flex-col gap-3 pt-6">
          <SectionTitle>Face-à-face</SectionTitle>
          {data.headToHead.length === 0 ? (
            <p className="text-sm text-muted-foreground">Pas de confrontation récente entre ces deux équipes.</p>
          ) : (
            <>
              <div className="flex items-center justify-center gap-4 font-heading">
                {teams.map((t, i) => (
                  <span key={t.id} className={cn("flex items-center gap-2", i === 1 && "flex-row-reverse")}>
                    <TeamLogo abbreviation={t.abbreviation} logoUrl={t.logoUrl} size={28} />
                    <span className="text-2xl font-bold tabular-nums">{wins(t.abbreviation)}</span>
                  </span>
                ))}
              </div>
              <p className="-mt-1 text-center text-[11px] text-muted-foreground">
                victoires sur les {data.headToHead.length} dernières confrontations
              </p>
              <ul className="flex flex-col">
                {data.headToHead.map((g) => {
                  const homeWon = g.homeScore > g.awayScore;
                  return (
                    <li key={g.date} className="flex items-center gap-2 border-t border-tint/10 py-2 text-sm first:border-t-0">
                      <span className="w-20 shrink-0 font-mono text-[11px] text-muted-foreground">
                        {formatMatchDate(new Date(g.date))}
                      </span>
                      <span className={cn("flex-1 text-right", !homeWon && "font-bold")}>{g.awayAbbreviation}</span>
                      <span className="font-mono tabular-nums">
                        {g.awayScore} - {g.homeScore}
                      </span>
                      <span className={cn("flex-1", homeWon && "font-bold")}>{g.homeAbbreviation}</span>
                      {g.playoffs && (
                        <span className="rounded bg-paint/15 px-1.5 text-[10px] font-bold uppercase text-paint">Playoffs</span>
                      )}
                    </li>
                  );
                })}
              </ul>
            </>
          )}
        </CardContent>
      </Card>
    </>
  );
}
