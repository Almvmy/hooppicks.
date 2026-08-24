"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Pencil, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { fetchAdminMatches, updateAdminMatch } from "@/lib/api/admin";
import { Match, MatchStatus } from "@/lib/types";
import { formatMatchDate, formatMatchTime } from "@/lib/utils";

const STATUS_OPTIONS: { value: MatchStatus | ""; label: string }[] = [
  { value: "", label: "Tous les statuts" },
  { value: "scheduled", label: "Programmé" },
  { value: "live", label: "En direct" },
  { value: "finished", label: "Terminé" },
];

function EditRow({ match, onDone }: { match: Match; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<MatchStatus>(match.status);
  const [homeScore, setHomeScore] = useState(match.homeScore ?? 0);
  const [awayScore, setAwayScore] = useState(match.awayScore ?? 0);

  const mutation = useMutation({
    mutationFn: () => updateAdminMatch(match.id, { status, homeScore, awayScore }),
    onSuccess: () => {
      toast.success("Match mis à jour.");
      queryClient.invalidateQueries({ queryKey: ["admin-matches"] });
      onDone();
    },
    onError: (err: Error) => toast.error(err.message || "Impossible de mettre à jour le match."),
  });

  return (
    <TableRow className="bg-primary/5">
      <TableCell colSpan={4}>
        <div className="flex flex-wrap items-end gap-2">
          <span className="text-sm font-medium">
            {match.awayTeam.abbreviation} @ {match.homeTeam.abbreviation}
          </span>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value as MatchStatus)}
            className="glass-inset h-8 rounded-md px-2 text-sm"
          >
            <option value="scheduled">Programmé</option>
            <option value="live">En direct</option>
            <option value="finished">Terminé</option>
          </select>
          <Input
            type="number"
            value={awayScore}
            onChange={(e) => setAwayScore(Number(e.target.value))}
            className="w-20"
            aria-label="Score visiteur"
          />
          <span className="text-muted-foreground">–</span>
          <Input
            type="number"
            value={homeScore}
            onChange={(e) => setHomeScore(Number(e.target.value))}
            className="w-20"
            aria-label="Score domicile"
          />
          <Button size="sm" disabled={mutation.isPending} onClick={() => mutation.mutate()}>
            {mutation.isPending ? "..." : "Enregistrer"}
          </Button>
          <Button size="sm" variant="outline" onClick={onDone}>
            Annuler
          </Button>
        </div>
      </TableCell>
    </TableRow>
  );
}

export function AdminMatchesPanel() {
  const [search, setSearch] = useState("");
  const [submittedSearch, setSubmittedSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<MatchStatus | "">("");
  const [editingId, setEditingId] = useState<string | null>(null);

  const { data: matches, isLoading, isError } = useQuery({
    queryKey: ["admin-matches", submittedSearch, statusFilter],
    queryFn: () => fetchAdminMatches(submittedSearch || undefined, statusFilter || undefined),
  });

  return (
    <Card>
      <CardContent className="flex flex-col gap-4 pt-6">
        <p className="text-xs uppercase tracking-wide text-muted-foreground">Matchs</p>

        <div className="flex flex-wrap gap-2">
          <form
            className="flex gap-2"
            onSubmit={(e) => {
              e.preventDefault();
              setSubmittedSearch(search.trim());
            }}
          >
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Chercher une équipe..."
              className="max-w-xs"
            />
            <Button type="submit" variant="outline" size="icon" aria-label="Rechercher">
              <Search className="h-4 w-4" />
            </Button>
          </form>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as MatchStatus | "")}
            className="glass-inset h-9 rounded-md px-2 text-sm"
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {isLoading && <BasketballLoader label="Chargement des matchs..." />}
        {isError && <p className="text-sm text-destructive">Impossible de charger les matchs.</p>}

        {matches && (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Match</TableHead>
                  <TableHead>Date</TableHead>
                  <TableHead>Statut</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {matches.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center text-muted-foreground">
                      Aucun match trouvé.
                    </TableCell>
                  </TableRow>
                )}
                {matches.map((match) =>
                  editingId === match.id ? (
                    <EditRow key={match.id} match={match} onDone={() => setEditingId(null)} />
                  ) : (
                    <TableRow key={match.id}>
                      <TableCell className="font-medium">
                        {match.awayTeam.abbreviation}
                        {match.awayScore != null && ` ${match.awayScore}`}
                        <span className="mx-1 text-muted-foreground">@</span>
                        {match.homeTeam.abbreviation}
                        {match.homeScore != null && ` ${match.homeScore}`}
                      </TableCell>
                      <TableCell className="font-mono text-xs text-muted-foreground">
                        {formatMatchDate(new Date(match.date))} · {formatMatchTime(new Date(match.date))}
                      </TableCell>
                      <TableCell className="capitalize">{match.status}</TableCell>
                      <TableCell className="text-right">
                        <Button variant="outline" size="icon" onClick={() => setEditingId(match.id)}>
                          <Pencil className="h-4 w-4" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  )
                )}
              </TableBody>
            </Table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
