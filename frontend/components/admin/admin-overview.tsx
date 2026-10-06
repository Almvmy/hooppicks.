"use client";

import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, CheckCircle2, Coins, Lock, Ticket, UserPlus, Users } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { MATCH_TYPE_META } from "@/components/match-stage-badge";
import { fetchAdminOverview } from "@/lib/api/admin";
import { AdminOverview as Overview, MatchType } from "@/lib/types";
import { cn, formatRelativeTime, formatShortDateTime } from "@/lib/utils";

function Kpi({ label, value, hint, icon: Icon }: { label: string; value: string; hint?: string; icon: typeof Users }) {
  return (
    <Card>
      <CardContent className="flex flex-col gap-1 pt-5">
        <p className="flex items-center gap-1.5 text-xs uppercase tracking-wide text-muted-foreground">
          <Icon className="h-3.5 w-3.5" />
          {label}
        </p>
        <p className="font-mono text-2xl font-bold">{value}</p>
        {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
      </CardContent>
    </Card>
  );
}

/** Barres journalières sur 14 jours, en CSS : pas besoin d'une lib de graphiques pour ça. */
function DailyBars({ series, field, label, className }: { series: Overview["series"]; field: "signups" | "bets"; label: string; className: string }) {
  const max = Math.max(1, ...series.map((d) => d[field]));
  const total = series.reduce((sum, d) => sum + d[field], 0);
  return (
    <div className="flex flex-col gap-2">
      <p className="flex items-baseline justify-between text-sm">
        <span className="font-medium">{label}</span>
        <span className="font-mono text-xs text-muted-foreground">{total} sur 14 j</span>
      </p>
      <div className="flex h-24 items-end gap-1" role="img" aria-label={`${label} : ${total} sur 14 jours`}>
        {series.map((d) => (
          <div key={d.date} className="group relative flex h-full flex-1 items-end">
            <div
              className={cn("w-full rounded-t-sm transition-opacity group-hover:opacity-80", className)}
              style={{ height: `${Math.max(d[field] === 0 ? 0 : 6, (d[field] / max) * 100)}%` }}
            />
            <span className="pointer-events-none absolute -top-6 left-1/2 hidden -translate-x-1/2 whitespace-nowrap rounded bg-foreground px-1.5 py-0.5 text-[10px] text-background group-hover:block">
              {d.date.slice(8, 10)}/{d.date.slice(5, 7)} : {d[field]}
            </span>
          </div>
        ))}
      </div>
      <div className="flex justify-between font-mono text-[10px] text-muted-foreground">
        <span>{series[0]?.date.slice(8, 10)}/{series[0]?.date.slice(5, 7)}</span>
        <span>aujourd&apos;hui</span>
      </div>
    </div>
  );
}

function HealthRow({ ok, label, detail }: { ok: boolean; label: string; detail: string }) {
  const Icon = ok ? CheckCircle2 : AlertTriangle;
  return (
    <div className="flex items-start gap-2 text-sm">
      <Icon className={cn("mt-0.5 h-4 w-4 shrink-0", ok ? "text-success" : "text-amber-500 light:text-amber-700")} />
      <div>
        <p className="font-medium">{label}</p>
        <p className="text-xs text-muted-foreground">{detail}</p>
      </div>
    </div>
  );
}

export function AdminOverview() {
  const { data, isLoading, isError, dataUpdatedAt } = useQuery({
    queryKey: ["admin-overview"],
    queryFn: fetchAdminOverview,
    refetchInterval: 60 * 1000,
  });

  if (isLoading) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <Skeleton key={i} className="h-24 rounded-2xl" />
        ))}
      </div>
    );
  }
  if (isError || !data) {
    return <p className="text-sm text-destructive">Impossible de charger la vue d&apos;ensemble.</p>;
  }

  const fr = (n: number) => n.toLocaleString("fr-FR");
  // Une synchro planifiée tourne toutes les 5 min : au-delà de 15 min sans
  // nouvelle, quelque chose bloque (quota balldontlie, serveur relancé…).
  // Comparée à l'heure de la dernière réponse du serveur : rendu pur.
  const lastSync = data.lastSyncAt ? new Date(data.lastSyncAt).getTime() : null;
  const syncFresh = lastSync !== null && dataUpdatedAt - lastSync < 15 * 60 * 1000;

  return (
    <div className="flex flex-col gap-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Kpi label="Joueurs" value={fr(data.totalUsers)} hint={`+${data.newUsers7d} cette semaine`} icon={Users} />
        <Kpi label="Actifs (7 j)" value={fr(data.activeBettors7d)} hint="ont posé au moins un pari" icon={UserPlus} />
        <Kpi label="Paris (7 j)" value={fr(data.betsPlaced7d)} hint={`${data.betsPlaced24h} sur 24 h · ${data.pendingBets} en attente`} icon={Ticket} />
        <Kpi
          label="Points en circulation"
          value={fr(data.pointsInCirculation)}
          hint={`${fr(data.pointsStaked7d)} misés cette semaine`}
          icon={Coins}
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardContent className="grid gap-6 pt-6 sm:grid-cols-2">
            <DailyBars series={data.series} field="signups" label="Inscriptions" className="bg-paint" />
            <DailyBars series={data.series} field="bets" label="Paris posés" className="bg-brand" />
          </CardContent>
        </Card>

        <Card>
          <CardContent className="flex flex-col gap-3 pt-6">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Santé des données</p>
            <HealthRow
              ok={syncFresh}
              label="Synchro des matchs"
              detail={
                data.lastSyncAt
                  ? `${formatRelativeTime(data.lastSyncAt)} (${formatShortDateTime(new Date(data.lastSyncAt))}) · ${data.lastGamesSynced} match(s), ${data.lastBetsResolved} pari(s) réglé(s) · ${data.syncMode ?? ""}`
                  : "Aucune synchro depuis le démarrage du serveur."
              }
            />
            <HealthRow
              ok={data.unlinkedMatches === 0}
              label="Liaison ESPN"
              detail={
                data.unlinkedMatches === 0
                  ? "Tous les matchs sont reliés à ESPN."
                  : `${data.unlinkedMatches} match(s) pas encore relié(s) : pas de feuille de match ni de phase.`
              }
            />
            <HealthRow
              ok={data.playersWithoutStats === 0}
              label="Stats joueurs"
              detail={`${fr(data.rosterPlayers - data.playersWithoutStats)} / ${fr(data.rosterPlayers)} joueurs à jour (rafraîchis par lots en continu).`}
            />
            <div className="flex items-start gap-2 text-sm">
              <Lock className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
              <div>
                <p className="font-medium">{data.lockedMatches} match(s) verrouillé(s)</p>
                <p className="text-xs text-muted-foreground">Corrigés à la main, ignorés par la synchro.</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardContent className="flex flex-col gap-3 pt-6">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Matchs en base : {fr(data.totalMatches)}</p>
          <div className="flex flex-wrap gap-2">
            {Object.entries(data.matchesByType).map(([type, count]) => (
              <span key={type} className="glass-inset-quiet rounded-full px-3 py-1 text-sm">
                {type === "unknown" ? "Phase inconnue" : MATCH_TYPE_META[type as MatchType]?.label ?? type}
                <span className="ml-1.5 font-mono font-bold">{count}</span>
              </span>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
