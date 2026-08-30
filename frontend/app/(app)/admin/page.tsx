"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { RefreshCw, ShieldCheck, Users2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { fetchProfile } from "@/lib/api/auth";
import {
  fetchAdminStatus,
  resolveBets,
  syncGames,
  syncPlayerStatsBatch,
  syncRosters,
  syncStandings,
  syncTeams,
} from "@/lib/api/admin";
import { AdminUsersPanel } from "@/components/admin/admin-users-panel";
import { AdminMatchesPanel } from "@/components/admin/admin-matches-panel";
import { AdminPendingBetsPanel } from "@/components/admin/admin-pending-bets-panel";

export default function AdminPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [daysAhead, setDaysAhead] = useState(3);
  const [startDate, setStartDate] = useState("");

  const profileQuery = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });

  useEffect(() => {
    if (profileQuery.data && !profileQuery.data.isAdmin) {
      router.replace("/dashboard");
    }
  }, [profileQuery.data, router]);

  const statusQuery = useQuery({
    queryKey: ["admin-status"],
    queryFn: fetchAdminStatus,
    enabled: profileQuery.data?.isAdmin === true,
  });

  const refreshStatus = () => queryClient.invalidateQueries({ queryKey: ["admin-status"] });

  const syncTeamsMutation = useMutation({
    mutationFn: syncTeams,
    onSuccess: (r) => {
      toast.success(`${r.teamsSynced} équipe(s) synchronisée(s).`);
      refreshStatus();
    },
    onError: () => toast.error("Échec de la synchro des équipes."),
  });

  const syncGamesMutation = useMutation({
    mutationFn: () => syncGames(daysAhead, startDate || undefined),
    onSuccess: (r) => {
      toast.success(`${r.gamesSynced} match(s) synchronisé(s).`);
      refreshStatus();
    },
    onError: () => toast.error("Échec de la synchro des matchs."),
  });

  const resolveBetsMutation = useMutation({
    mutationFn: resolveBets,
    onSuccess: (r) => {
      toast.success(`${r.resolved} pari(s) résolu(s).`);
      refreshStatus();
      queryClient.invalidateQueries({ queryKey: ["admin-pending-bets"] });
    },
    onError: () => toast.error("Échec de la résolution des paris."),
  });

  const syncRostersMutation = useMutation({
    mutationFn: syncRosters,
    onSuccess: () => {
      toast.success("Effectifs synchronisés.");
      queryClient.invalidateQueries({ queryKey: ["team-roster"] });
    },
    onError: () => toast.error("Échec de la synchro des effectifs."),
  });

  const syncStandingsMutation = useMutation({
    mutationFn: syncStandings,
    onSuccess: () => {
      toast.success("Classement synchronisé.");
      queryClient.invalidateQueries({ queryKey: ["teams", "rankings"] });
    },
    onError: () => toast.error("Échec de la synchro du classement."),
  });

  const syncPlayerStatsMutation = useMutation({
    mutationFn: syncPlayerStatsBatch,
    onSuccess: () => {
      toast.success("Lot de stats joueurs synchronisé.");
      queryClient.invalidateQueries({ queryKey: ["team-roster"] });
    },
    onError: () => toast.error("Échec de la synchro des stats joueurs."),
  });

  const [confirmResolve, setConfirmResolve] = useState(false);

  if (profileQuery.isLoading || !profileQuery.data?.isAdmin) {
    return <BasketballLoader label="Vérification des accès..." />;
  }

  const status = statusQuery.data;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center gap-2">
        <ShieldCheck className="h-6 w-6 text-primary" />
        <h1 className="font-heading text-2xl font-bold">Console admin</h1>
      </div>

      {statusQuery.isError && (
        <p className="text-sm text-destructive">
          Impossible de charger le statut : les tirets ci-dessous ne veulent pas dire "zéro", la donnée n&apos;a simplement pas pu être récupérée.
        </p>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardContent className="flex flex-col gap-1 pt-6">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Utilisateurs</p>
            {statusQuery.isLoading ? (
              <Skeleton className="h-7 w-12" />
            ) : (
              <p className="font-mono text-2xl font-bold">{status?.totalUsers ?? "-"}</p>
            )}
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex flex-col gap-1 pt-6">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Matchs en base</p>
            {statusQuery.isLoading ? (
              <Skeleton className="h-7 w-12" />
            ) : (
              <p className="font-mono text-2xl font-bold">{status?.totalMatches ?? "-"}</p>
            )}
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex flex-col gap-1 pt-6">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Paris en attente</p>
            {statusQuery.isLoading ? (
              <Skeleton className="h-7 w-12" />
            ) : (
              <p className="font-mono text-2xl font-bold">{status?.pendingBets ?? "-"}</p>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardContent className="flex flex-col gap-2 pt-6">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Dernière synchro</p>
          {statusQuery.isLoading ? (
            <Skeleton className="h-5 w-64" />
          ) : status?.lastSyncAt ? (
            <p className="text-sm">
              {new Date(status.lastSyncAt).toLocaleDateString("fr-FR", {
                day: "2-digit",
                month: "short",
                hour: "2-digit",
                minute: "2-digit",
              })}
              {" · "}
              <span className="font-mono">{status.lastGamesSynced}</span> match(s),{" "}
              <span className="font-mono">{status.lastBetsResolved}</span> pari(s) résolu(s)
              {" ("}
              {status.syncMode}
              {")"}
            </p>
          ) : (
            <p className="text-sm text-muted-foreground">Aucune synchro depuis le démarrage du serveur.</p>
          )}
        </CardContent>
      </Card>

      <div className="grid gap-4 sm:grid-cols-2">
        <Card>
          <CardContent className="flex flex-col gap-3 pt-6">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Équipes</p>
            <Button
              variant="outline"
              className="gap-1.5"
              onClick={() => syncTeamsMutation.mutate()}
              disabled={syncTeamsMutation.isPending}
            >
              <RefreshCw className="h-4 w-4" />
              Synchroniser les équipes
            </Button>
            <Button
              variant="outline"
              className="gap-1.5"
              onClick={() => syncRostersMutation.mutate()}
              disabled={syncRostersMutation.isPending}
            >
              <RefreshCw className="h-4 w-4" />
              Synchroniser les effectifs (ESPN)
            </Button>
            <Button
              variant="outline"
              className="gap-1.5"
              onClick={() => syncStandingsMutation.mutate()}
              disabled={syncStandingsMutation.isPending}
            >
              <RefreshCw className="h-4 w-4" />
              Synchroniser le classement (ESPN)
            </Button>
            <Button
              variant="outline"
              className="gap-1.5"
              onClick={() => syncPlayerStatsMutation.mutate()}
              disabled={syncPlayerStatsMutation.isPending}
            >
              <RefreshCw className="h-4 w-4" />
              Avancer un lot de stats joueurs (ESPN)
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="flex flex-col gap-3 pt-6">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Matchs</p>
            <div className="flex gap-2">
              <Input
                type="number"
                min={0}
                max={30}
                value={daysAhead}
                onChange={(e) => setDaysAhead(Number(e.target.value))}
                className="w-20"
              />
              <Input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                placeholder="Date de départ"
              />
            </div>
            <Button
              variant="outline"
              className="gap-1.5"
              onClick={() => syncGamesMutation.mutate()}
              disabled={syncGamesMutation.isPending}
            >
              <RefreshCw className="h-4 w-4" />
              Synchroniser les matchs
            </Button>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardContent className="flex flex-col gap-3 pt-6">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Paris</p>
          <Button
            variant="outline"
            className="w-fit gap-1.5"
            onClick={() => setConfirmResolve(true)}
            disabled={resolveBetsMutation.isPending}
          >
            <Users2 className="h-4 w-4" />
            Résoudre les paris en attente
          </Button>
        </CardContent>
      </Card>

      <AdminPendingBetsPanel />
      <AdminMatchesPanel />
      <AdminUsersPanel />

      <AlertDialog open={confirmResolve} onOpenChange={setConfirmResolve}>
        <AlertDialogContent>
          <AlertDialogTitle>Résoudre les paris en attente ?</AlertDialogTitle>
          <AlertDialogDescription>
            Tous les paris en attente dont le match est terminé seront réglés immédiatement (gains crédités,
            mises perdues débitées). Cette action est immédiate et ne peut pas être annulée.
          </AlertDialogDescription>
          <AlertDialogFooter>
            <AlertDialogCancel>Annuler</AlertDialogCancel>
            <AlertDialogAction onClick={() => resolveBetsMutation.mutate()}>Confirmer</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
