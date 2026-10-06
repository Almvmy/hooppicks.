"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { syncGames, syncPlayerStatsBatch, syncRosters, syncStandings, syncTeams } from "@/lib/api/admin";

function SyncAction({
  title,
  description,
  pending,
  onRun,
  children,
}: {
  title: string;
  description: string;
  pending: boolean;
  onRun: () => void;
  children?: React.ReactNode;
}) {
  return (
    <div className="glass-inset-quiet flex flex-col gap-2 rounded-2xl p-4">
      <p className="font-medium">{title}</p>
      <p className="text-xs text-muted-foreground">{description}</p>
      {children}
      <Button variant="outline" className="mt-auto w-fit gap-1.5" onClick={onRun} disabled={pending}>
        <RefreshCw className={pending ? "h-4 w-4 animate-spin" : "h-4 w-4"} />
        {pending ? "En cours…" : "Lancer"}
      </Button>
    </div>
  );
}

/**
 * Synchros manuelles. Tout tourne déjà seul (matchs toutes les 5 min,
 * effectifs et classement chaque matin) : ces boutons servent à rattraper
 * un trou ou à tester, pas au quotidien.
 */
export function AdminSyncPanel() {
  const queryClient = useQueryClient();
  const [daysAhead, setDaysAhead] = useState(3);
  const [startDate, setStartDate] = useState("");

  const after = () => {
    queryClient.invalidateQueries({ queryKey: ["admin-overview"] });
    queryClient.invalidateQueries({ queryKey: ["admin-audit"] });
  };

  const games = useMutation({
    mutationFn: () => syncGames(daysAhead, startDate || undefined),
    onSuccess: (r) => {
      toast.success(`${r.gamesSynced} match(s) synchronisé(s).`);
      after();
      queryClient.invalidateQueries({ queryKey: ["matches"] });
    },
    onError: () => toast.error("Échec de la synchro des matchs."),
  });
  const teams = useMutation({
    mutationFn: syncTeams,
    onSuccess: (r) => {
      toast.success(`${r.teamsSynced} équipe(s) synchronisée(s).`);
      after();
    },
    onError: () => toast.error("Échec de la synchro des équipes."),
  });
  const rosters = useMutation({
    mutationFn: syncRosters,
    onSuccess: () => {
      toast.success("Effectifs synchronisés.");
      after();
      queryClient.invalidateQueries({ queryKey: ["team-roster"] });
    },
    onError: () => toast.error("Échec de la synchro des effectifs."),
  });
  const standings = useMutation({
    mutationFn: syncStandings,
    onSuccess: () => {
      toast.success("Classement synchronisé.");
      after();
      queryClient.invalidateQueries({ queryKey: ["teams", "rankings"] });
    },
    onError: () => toast.error("Échec de la synchro du classement."),
  });
  const playerStats = useMutation({
    mutationFn: syncPlayerStatsBatch,
    onSuccess: () => {
      toast.success("Lot de stats joueurs synchronisé.");
      after();
    },
    onError: () => toast.error("Échec de la synchro des stats joueurs."),
  });

  return (
    <Card>
      <CardContent className="flex flex-col gap-4 pt-6">
        <p className="text-sm text-muted-foreground">
          Tout se synchronise déjà seul : matchs et paris toutes les 5 minutes, effectifs et classement chaque matin,
          stats joueurs par lots en continu. Ces boutons servent à rattraper un trou ou à tester.
        </p>
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          <SyncAction
            title="Matchs (balldontlie)"
            description="Scores, statuts et cotes sur une période, puis résolution des paris des matchs terminés. Les matchs verrouillés ne sont pas touchés."
            pending={games.isPending}
            onRun={() => games.mutate()}
          >
            <div className="flex flex-wrap gap-2">
              <label className="flex flex-col gap-1 text-xs text-muted-foreground">
                Jours
                <Input type="number" min={0} max={30} value={daysAhead} onChange={(e) => setDaysAhead(Number(e.target.value))} className="w-20" />
              </label>
              <label className="flex flex-col gap-1 text-xs text-muted-foreground">
                À partir du (défaut : hier)
                <Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
              </label>
            </div>
          </SyncAction>
          <SyncAction
            title="Équipes (balldontlie)"
            description="Liste des 30 franchises. À relancer seulement si une équipe manque."
            pending={teams.isPending}
            onRun={() => teams.mutate()}
          />
          <SyncAction
            title="Effectifs (ESPN)"
            description="Joueurs actuels, photos et blessures des 30 équipes. Automatique chaque jour à 6 h."
            pending={rosters.isPending}
            onRun={() => rosters.mutate()}
          />
          <SyncAction
            title="Classement officiel (ESPN)"
            description="Bilans, séries et places en conférence. Automatique chaque jour à 6 h 05."
            pending={standings.isPending}
            onRun={() => standings.mutate()}
          />
          <SyncAction
            title="Stats joueurs (ESPN)"
            description="Avance d'un seul lot (~8 joueurs). Le reste tourne en continu en tâche de fond."
            pending={playerStats.isPending}
            onRun={() => playerStats.mutate()}
          />
        </div>
      </CardContent>
    </Card>
  );
}
