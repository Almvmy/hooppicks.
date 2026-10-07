"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { fetchSeasonPicks, setSeasonResult } from "@/lib/api/season-picks";
import { useTeamsByAbbreviation } from "@/lib/use-teams";

/**
 * Saisie des bonnes réponses des pronostics de saison, à faire quand on les
 * connaît (fin de saison régulière, finales). Modifiable en cas d'erreur ;
 * chaque saisie va au journal d'audit.
 */
export function AdminSeasonResults() {
  const queryClient = useQueryClient();
  const teams = [...useTeamsByAbbreviation().values()].sort((a, b) => a.name.localeCompare(b.name));
  const { data, isLoading, isError } = useQuery({ queryKey: ["season-picks"], queryFn: fetchSeasonPicks });
  const [draft, setDraft] = useState<Record<string, string>>({});

  const mutation = useMutation({
    mutationFn: ({ question, team }: { question: string; team: string }) => setSeasonResult(question, team),
    onSuccess: () => {
      toast.success("Résultat enregistré.");
      queryClient.invalidateQueries({ queryKey: ["season-picks"] });
    },
    onError: (error) => toast.error(error instanceof Error && error.message ? error.message : "Enregistrement impossible."),
  });

  if (isError) return <p className="text-destructive">Impossible de charger les pronostics de saison.</p>;
  if (isLoading || !data) return null;

  return (
    <Card>
      <CardContent className="flex flex-col gap-4">
        <div>
          <h2 className="font-heading text-base font-bold">Résultats des pronostics de saison {data.season}</h2>
          <p className="text-sm text-muted-foreground">À saisir une fois la réponse connue : le classement des pronostics de saison se met à jour aussitôt.</p>
        </div>
        {data.questions.map((q) => {
          const value = draft[q.key] ?? data.results[q.key] ?? "";
          return (
            <div key={q.key} className="flex flex-wrap items-center gap-2">
              <label htmlFor={`season-${q.key}`} className="w-56 text-sm">
                {q.label}
              </label>
              <select
                id={`season-${q.key}`}
                value={value}
                onChange={(e) => setDraft((d) => ({ ...d, [q.key]: e.target.value }))}
                className="glass-inset rounded-lg px-2 py-1.5 text-sm"
              >
                <option value="">Pas encore connu</option>
                {teams.map((t) => (
                  <option key={t.abbreviation} value={t.abbreviation}>
                    {t.name}
                  </option>
                ))}
              </select>
              <Button
                size="sm"
                variant="outline"
                disabled={!value || value === data.results[q.key] || mutation.isPending}
                onClick={() => mutation.mutate({ question: q.key, team: value })}
              >
                Enregistrer
              </Button>
            </div>
          );
        })}
      </CardContent>
    </Card>
  );
}
