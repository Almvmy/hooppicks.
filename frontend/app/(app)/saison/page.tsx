"use client";

import { useState } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Check, Lock, Medal, X } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { TeamLogo } from "@/components/team-logo";
import { fetchSeasonPicks, saveSeasonPick, SeasonQuestion } from "@/lib/api/season-picks";
import { useTeamsByAbbreviation } from "@/lib/use-teams";
import { TeamRank } from "@/lib/types";
import { cn, formatMatchDate, formatMatchTime } from "@/lib/utils";

// Les finalistes se choisissent dans leur conférence.
const CONFERENCE_OF: Partial<Record<SeasonQuestion["key"], TeamRank["conference"]>> = {
  east_finalist: "Est",
  west_finalist: "Ouest",
};

function TeamGrid({
  teams,
  selected,
  disabled,
  onPick,
}: {
  teams: TeamRank[];
  selected: string | undefined;
  disabled: boolean;
  onPick: (abbr: string) => void;
}) {
  return (
    <div className="grid grid-cols-5 gap-1.5 sm:grid-cols-8 lg:grid-cols-10">
      {teams.map((t) => (
        <button
          key={t.abbreviation}
          type="button"
          title={t.name}
          aria-label={t.name}
          aria-pressed={selected === t.abbreviation}
          disabled={disabled}
          onClick={() => onPick(t.abbreviation)}
          className={cn(
            "flex flex-col items-center gap-1 rounded-xl p-1.5 text-[10px] font-semibold transition-all",
            selected === t.abbreviation ? "glass-accent" : selected ? "opacity-45 hover:opacity-100" : "hover:bg-tint/[0.06]",
            disabled && "cursor-default hover:opacity-45"
          )}
        >
          <TeamLogo abbreviation={t.abbreviation} logoUrl={t.logoUrl} size={30} />
          {t.abbreviation}
        </button>
      ))}
    </div>
  );
}

export default function SeasonPicksPage() {
  const queryClient = useQueryClient();
  const teamsMap = useTeamsByAbbreviation();
  const teams = [...teamsMap.values()].sort((a, b) => a.name.localeCompare(b.name));
  const { data, isLoading, isError } = useQuery({ queryKey: ["season-picks"], queryFn: fetchSeasonPicks });
  const [open, setOpen] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: ({ question, team }: { question: string; team: string }) => saveSeasonPick(question, team),
    onSuccess: () => {
      setOpen(null);
      queryClient.invalidateQueries({ queryKey: ["season-picks"] });
    },
    onError: (error) => toast.error(error instanceof Error && error.message ? error.message : "Pronostic non enregistré."),
  });

  const deadline = data?.deadline ? new Date(data.deadline) : null;
  const total = data?.questions.reduce((sum, q) => sum + q.points, 0) ?? 0;

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="flex items-center gap-2.5 font-heading text-2xl font-bold">
          <Medal className="h-6 w-6 text-primary" />
          Pronostics de saison {data?.season}
        </h1>
        <p className="mt-1 text-muted-foreground">
          Quatre questions sur toute la saison, jusqu&apos;à {total} points à la clé, dans un classement à part.{" "}
          {data?.locked
            ? "Les pronostics sont clos."
            : deadline
              ? `Modifiables jusqu'au ${formatMatchDate(deadline)} à ${formatMatchTime(deadline)}, une semaine après le début de la saison régulière.`
              : "Modifiables jusqu'à une semaine après le début de la saison régulière."}
        </p>
      </div>

      {isError && <p className="text-destructive">Impossible de charger les pronostics de saison. Réessaie plus tard.</p>}
      {isLoading && <Skeleton className="h-60 w-full rounded-2xl" />}

      {data && (
        <div className="grid gap-4 lg:grid-cols-2">
          {data.questions.map((q) => {
            const mine = data.myPicks[q.key];
            const result = data.results[q.key];
            const team = mine ? teamsMap.get(mine) : undefined;
            const conference = CONFERENCE_OF[q.key];
            const choices = conference ? teams.filter((t) => t.conference === conference) : teams;
            const split = data.community[q.key];
            return (
              <Card key={q.key}>
                <CardContent className="flex flex-col gap-3">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h2 className="font-heading text-base font-bold">{q.label}</h2>
                      <p className="text-xs text-muted-foreground">{q.points} points si c&apos;est juste</p>
                    </div>
                    {result ? (
                      mine === result ? (
                        <span className="flex items-center gap-1 text-sm font-semibold text-success">
                          <Check className="h-4 w-4" /> +{q.points}
                        </span>
                      ) : (
                        <span className="flex items-center gap-1 text-sm text-muted-foreground">
                          <X className="h-4 w-4" /> {result}
                        </span>
                      )
                    ) : (
                      data.locked && <Lock className="h-4 w-4 text-muted-foreground" />
                    )}
                  </div>

                  <div className="flex items-center justify-between gap-3">
                    {team ? (
                      <span className="flex items-center gap-2 font-semibold">
                        <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={32} />
                        {team.name}
                      </span>
                    ) : (
                      <span className="text-sm text-muted-foreground">{data.locked ? "Pas de pronostic." : "Pas encore choisi."}</span>
                    )}
                    {!data.locked && (
                      <button
                        type="button"
                        className="text-sm font-semibold text-primary hover:underline"
                        onClick={() => setOpen(open === q.key ? null : q.key)}
                      >
                        {open === q.key ? "Fermer" : mine ? "Changer" : "Choisir"}
                      </button>
                    )}
                  </div>

                  {open === q.key && !data.locked && (
                    <TeamGrid
                      teams={choices}
                      selected={mine}
                      disabled={mutation.isPending}
                      onPick={(abbr) => mutation.mutate({ question: q.key, team: abbr })}
                    />
                  )}

                  {split && Object.keys(split).length > 0 && (
                    <div className="flex flex-wrap gap-1.5 text-xs text-muted-foreground">
                      {Object.entries(split)
                        .slice(0, 4)
                        .map(([abbr, pct]) => (
                          <span key={abbr} className="glass-inset-quiet rounded-full px-2 py-0.5 font-mono">
                            {abbr} {pct}%
                          </span>
                        ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {data && data.leaderboard.length > 0 && (
        <section className="flex flex-col gap-3">
          <h2 className="font-heading text-sm font-bold uppercase tracking-wide text-muted-foreground">Classement des pronostics de saison</h2>
          <Card>
            <CardContent className="divide-y divide-tint/10">
              {data.leaderboard.map((s, i) => (
                <div key={s.username} className="flex items-center justify-between gap-3 py-2">
                  <span className="flex items-center gap-3">
                    <span className="w-6 font-mono text-sm text-muted-foreground">{i + 1}</span>
                    <Link href={`/u/${encodeURIComponent(s.username)}`} className="font-semibold hover:underline">
                      @{s.username}
                    </Link>
                  </span>
                  <span className="font-mono text-sm">
                    <span className="font-bold">{s.points} pts</span>
                    <span className="ml-2 text-muted-foreground">{s.correct} juste{s.correct > 1 ? "s" : ""}</span>
                  </span>
                </div>
              ))}
            </CardContent>
          </Card>
        </section>
      )}
    </div>
  );
}
