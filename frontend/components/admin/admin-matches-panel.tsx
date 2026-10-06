"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Ban, Lock, Pencil, Search, Unlock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { MatchStageBadge } from "@/components/match-stage-badge";
import { ReasonDialog } from "@/components/admin/reason-dialog";
import { fetchAdminMatches, unlockAdminMatch, updateAdminMatch, voidMatchBets } from "@/lib/api/admin";
import { AdminMatch, MatchStatus } from "@/lib/types";
import { cn, formatMatchDate, formatMatchTime } from "@/lib/utils";

const STATUS_OPTIONS: { value: MatchStatus | ""; label: string }[] = [
  { value: "", label: "Tous les statuts" },
  { value: "scheduled", label: "Programmé" },
  { value: "live", label: "En direct" },
  { value: "finished", label: "Terminé" },
];

const STATUS_LABEL: Record<MatchStatus, string> = { scheduled: "Programmé", live: "En direct", finished: "Terminé" };

const matchLabel = ({ match }: AdminMatch) => `${match.awayTeam.abbreviation} @ ${match.homeTeam.abbreviation}`;

function EditRow({ item, onDone }: { item: AdminMatch; onDone: () => void }) {
  const { match } = item;
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<MatchStatus>(match.status);
  const [homeScore, setHomeScore] = useState(match.homeScore ?? 0);
  const [awayScore, setAwayScore] = useState(match.awayScore ?? 0);

  const mutation = useMutation({
    mutationFn: () => updateAdminMatch(match.id, { status, homeScore, awayScore }),
    onSuccess: () => {
      toast.success("Match corrigé et verrouillé : la synchro ne l'écrasera pas.");
      queryClient.invalidateQueries({ queryKey: ["admin-matches"] });
      queryClient.invalidateQueries({ queryKey: ["admin-overview"] });
      onDone();
    },
    onError: (err: Error) => toast.error(err.message || "Impossible de mettre à jour le match."),
  });

  return (
    <TableRow className="bg-primary/5">
      <TableCell colSpan={5}>
        <div className="flex flex-col gap-2">
          <div className="flex flex-wrap items-end gap-2">
            <span className="text-sm font-medium">{matchLabel(item)}</span>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as MatchStatus)}
              className="glass-inset h-8 rounded-md px-2 text-sm"
              aria-label="Statut"
            >
              <option value="scheduled">Programmé</option>
              <option value="live">En direct</option>
              <option value="finished">Terminé</option>
            </select>
            <Input
              type="number"
              min={0}
              value={awayScore}
              onChange={(e) => setAwayScore(Number(e.target.value))}
              className="w-20"
              aria-label={`Score ${match.awayTeam.abbreviation} (extérieur)`}
            />
            <span className="text-muted-foreground">-</span>
            <Input
              type="number"
              min={0}
              value={homeScore}
              onChange={(e) => setHomeScore(Number(e.target.value))}
              className="w-20"
              aria-label={`Score ${match.homeTeam.abbreviation} (domicile)`}
            />
            <Button size="sm" disabled={mutation.isPending} onClick={() => mutation.mutate()}>
              {mutation.isPending ? "..." : "Enregistrer"}
            </Button>
            <Button size="sm" variant="outline" onClick={onDone}>
              Annuler
            </Button>
          </div>
          <p className="text-xs text-muted-foreground">
            Le match sera <strong>verrouillé</strong> : la synchro ne touchera plus à son statut ni à son score.
            {status === "finished" && item.pendingBets > 0 && (
              <> Ses {item.pendingBets} pari(s) en attente seront réglés à la prochaine résolution.</>
            )}
          </p>
          {item.resolvedBets > 0 && (
            <p className="text-xs font-medium text-destructive">
              Attention : {item.resolvedBets} pari(s) sont déjà réglés sur ce match. Corriger le score ne les
              recalcule pas ; au besoin, ajuste les soldes concernés depuis l&apos;onglet Utilisateurs.
            </p>
          )}
        </div>
      </TableCell>
    </TableRow>
  );
}

export function AdminMatchesPanel() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const [submittedSearch, setSubmittedSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<MatchStatus | "">("");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [voiding, setVoiding] = useState<AdminMatch | null>(null);

  const { data: matches, isLoading, isError } = useQuery({
    queryKey: ["admin-matches", submittedSearch, statusFilter],
    queryFn: () => fetchAdminMatches(submittedSearch || undefined, statusFilter || undefined),
  });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ["admin-matches"] });
    queryClient.invalidateQueries({ queryKey: ["admin-overview"] });
    queryClient.invalidateQueries({ queryKey: ["admin-audit"] });
  };

  const unlockMutation = useMutation({
    mutationFn: unlockAdminMatch,
    onSuccess: () => {
      toast.success("Match rendu à la synchro.");
      refresh();
    },
    onError: (err: Error) => toast.error(err.message || "Impossible de déverrouiller."),
  });

  const voidMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => voidMatchBets(id, reason),
    onSuccess: (r) => {
      toast.success(`${r.voided} pari(s) annulé(s) et remboursé(s).`);
      refresh();
      queryClient.invalidateQueries({ queryKey: ["admin-pending-bets"] });
    },
    onError: (err: Error) => toast.error(err.message || "Annulation impossible."),
  });

  return (
    <Card>
      <CardContent className="flex flex-col gap-4 pt-6">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Matchs · du plus récent au plus ancien (100 max)</p>
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
                placeholder="Équipe ou sigle (NYK)…"
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
              aria-label="Filtrer par statut"
            >
              {STATUS_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
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
                  <TableHead className="text-right">Paris</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {matches.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} className="text-center text-muted-foreground">
                      Aucun match trouvé.
                    </TableCell>
                  </TableRow>
                )}
                {matches.map((item) => {
                  const { match } = item;
                  if (editingId === match.id) {
                    return <EditRow key={match.id} item={item} onDone={() => setEditingId(null)} />;
                  }
                  return (
                    <TableRow key={match.id}>
                      <TableCell className="font-medium">
                        <span className="flex items-center gap-1.5">
                          {item.locked && (
                            <Lock className="h-3.5 w-3.5 shrink-0 text-primary" aria-label="Verrouillé (corrigé à la main)" />
                          )}
                          {match.awayTeam.abbreviation}
                          {match.awayScore != null && ` ${match.awayScore}`}
                          <span className="text-muted-foreground">@</span>
                          {match.homeTeam.abbreviation}
                          {match.homeScore != null && ` ${match.homeScore}`}
                        </span>
                        <MatchStageBadge match={match} className="mt-1" />
                      </TableCell>
                      <TableCell className="whitespace-nowrap font-mono text-xs text-muted-foreground">
                        {formatMatchDate(new Date(match.date))} · {formatMatchTime(new Date(match.date))}
                      </TableCell>
                      <TableCell>
                        <span
                          className={cn(
                            "text-sm",
                            match.status === "live" && "font-semibold text-live",
                            match.status === "finished" && "text-muted-foreground"
                          )}
                        >
                          {STATUS_LABEL[match.status]}
                        </span>
                      </TableCell>
                      <TableCell className="text-right font-mono text-xs">
                        <span title="En attente">{item.pendingBets}</span>
                        <span className="text-muted-foreground"> / </span>
                        <span title="Déjà réglés" className="text-muted-foreground">
                          {item.resolvedBets}
                        </span>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-1.5">
                          <Button variant="outline" size="icon" title="Corriger le score / statut" onClick={() => setEditingId(match.id)}>
                            <Pencil className="h-4 w-4" />
                          </Button>
                          {item.locked && (
                            <Button
                              variant="outline"
                              size="icon"
                              title="Déverrouiller (rendre à la synchro)"
                              disabled={unlockMutation.isPending}
                              onClick={() => unlockMutation.mutate(match.id)}
                            >
                              <Unlock className="h-4 w-4" />
                            </Button>
                          )}
                          {item.pendingBets > 0 && (
                            <Button
                              variant="outline"
                              size="icon"
                              title="Annuler et rembourser ses paris en attente"
                              className="text-destructive hover:text-destructive"
                              onClick={() => setVoiding(item)}
                            >
                              <Ban className="h-4 w-4" />
                            </Button>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
            <p className="mt-2 text-xs text-muted-foreground">
              Paris : en attente / déjà réglés. <Lock className="inline h-3 w-3" /> = corrigé à la main, ignoré par la synchro.
            </p>
          </div>
        )}
      </CardContent>

      <ReasonDialog
        open={voiding !== null}
        onOpenChange={(open) => !open && setVoiding(null)}
        title={voiding ? `Annuler les paris sur ${matchLabel(voiding)} ?` : ""}
        description={
          <>
            Les {voiding?.pendingBets} pari(s) en attente qui contiennent ce match seront annulés et leur mise
            remboursée, combinés compris. Pour un match reporté ou annulé. Action immédiate et définitive.
          </>
        }
        confirmLabel="Annuler et rembourser"
        destructive
        onConfirm={(reason) => {
          if (voiding) voidMutation.mutate({ id: voiding.match.id, reason });
          setVoiding(null);
        }}
      />
    </Card>
  );
}
