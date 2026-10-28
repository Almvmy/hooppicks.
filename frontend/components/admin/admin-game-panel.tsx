"use client";

import { useQuery } from "@tanstack/react-query";
import { AlertTriangle } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchAdminInsights, type AdminInsights } from "@/lib/api/admin";
import { cn } from "@/lib/utils";

// En dessous, l'écart entre réussite réelle et annoncée tient surtout du hasard.
const MIN_SAMPLE = 30;
// Écart (en points de %) à partir duquel une ligne est signalée.
const ALERT_GAP = 8;

const pct = (n: number) => `${n.toLocaleString("fr-FR", { maximumFractionDigits: 1 })} %`;
const dayMonth = (iso: string) => new Date(`${iso}T12:00:00Z`).toLocaleDateString("fr-FR", { day: "numeric", month: "short", timeZone: "UTC" });

function Activity({ weeks }: { weeks: AdminInsights["activity"] }) {
  if (weeks.length === 0) return <p className="text-sm text-muted-foreground">Pas encore de tickets.</p>;
  const max = Math.max(1, ...weeks.map((w) => w.activePlayers));
  const last = weeks[weeks.length - 1];
  const prev = weeks[weeks.length - 2];
  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <p className="text-sm font-medium">Joueurs actifs par semaine de jeu</p>
        <p className="text-xs text-muted-foreground">
          Cette semaine : <span className="font-mono font-bold text-foreground">{last.activePlayers}</span>
          {prev && prev.activePlayers > 0 && (
            <> · {Math.round((last.returningPlayers * 100) / prev.activePlayers)} % des joueurs de la semaine passée sont revenus</>
          )}
        </p>
      </div>
      <div className="flex h-32 items-end gap-2" role="img" aria-label="Joueurs actifs et revenus par semaine">
        {weeks.map((w) => (
          <div key={w.week} className="group relative flex h-full flex-1 flex-col justify-end">
            <div className="flex w-full flex-col justify-end overflow-hidden rounded-t-md" style={{ height: `${(w.activePlayers / max) * 100}%` }}>
              <div className="w-full bg-brand/40" style={{ flex: w.activePlayers - w.returningPlayers }} />
              <div className="w-full bg-brand" style={{ flex: w.returningPlayers }} />
            </div>
            <span className="pointer-events-none absolute -top-6 left-1/2 hidden -translate-x-1/2 whitespace-nowrap rounded bg-foreground px-1.5 py-0.5 text-[10px] text-background group-hover:block">
              {w.activePlayers} actifs · {w.returningPlayers} revenus · {w.tickets} tickets
            </span>
          </div>
        ))}
      </div>
      <div className="flex justify-between font-mono text-[10px] text-muted-foreground">
        <span>{dayMonth(weeks[0].week)}</span>
        <span className="flex items-center gap-3">
          <span className="flex items-center gap-1"><span className="h-2 w-2 rounded-sm bg-brand" /> revenus</span>
          <span className="flex items-center gap-1"><span className="h-2 w-2 rounded-sm bg-brand/40" /> nouveaux ou de retour</span>
        </span>
      </div>
    </div>
  );
}

/**
 * Onglet « Jeu » : activité semaine après semaine et équilibre des cotes.
 * L'équilibre compare la réussite réelle de chaque type de pari à celle que
 * ses cotes annonçaient : au-dessus, les cotes sont trop généreuses (on l'a vu
 * avec l'ancienne ligne fixe des totaux), en dessous, trop sévères.
 */
export function AdminGamePanel() {
  const { data, isLoading, isError } = useQuery({ queryKey: ["admin-insights"], queryFn: fetchAdminInsights, refetchInterval: 5 * 60_000 });

  if (isLoading) return <Skeleton className="h-72 w-full rounded-2xl" />;
  if (isError || !data) return <p className="text-sm text-destructive">Impossible de charger les chiffres du jeu.</p>;

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardContent className="pt-6">
          <Activity weeks={data.activity} />
        </CardContent>
      </Card>

      <Card>
        <CardContent className="flex flex-col gap-4 pt-6">
          <div>
            <p className="text-sm font-medium">Équilibre des cotes · {data.balanceDays} derniers jours</p>
            <p className="text-xs text-muted-foreground">
              Réussite réelle (barre pleine) face à celle annoncée par les cotes (trait). Tickets à une sélection, combinés à part.
            </p>
          </div>
          {data.balance.length === 0 && <p className="text-sm text-muted-foreground">Pas encore de ticket réglé.</p>}
          <div className="flex flex-col divide-y divide-tint/10">
            {data.balance.map((b) => {
              const gap = b.actualPct - b.expectedPct;
              const enough = b.tickets >= MIN_SAMPLE;
              const alert = enough && Math.abs(gap) >= ALERT_GAP;
              return (
                <div key={b.type} className="flex flex-col gap-1.5 py-3">
                  <div className="flex items-baseline justify-between gap-3 text-sm">
                    <span className="flex items-center gap-1.5 font-medium">
                      {alert && <AlertTriangle className="h-3.5 w-3.5 text-amber-500" aria-label="À surveiller" />}
                      {b.type}
                    </span>
                    <span className="font-mono text-xs text-muted-foreground">
                      {b.won}/{b.tickets} · <span className="font-bold text-foreground">{pct(b.actualPct)}</span> pour {pct(b.expectedPct)} annoncés
                    </span>
                  </div>
                  <div className="relative h-2 rounded-full bg-tint/10">
                    <div className={cn("h-full rounded-full", alert ? "bg-amber-500" : "bg-primary")} style={{ width: `${Math.min(100, b.actualPct)}%` }} />
                    <div className="absolute -top-1 h-4 w-0.5 rounded bg-foreground" style={{ left: `${Math.min(100, b.expectedPct)}%` }} aria-hidden />
                  </div>
                  <p className="text-[11px] text-muted-foreground">
                    {!enough
                      ? `Échantillon trop petit pour conclure (${MIN_SAMPLE} tickets au moins).`
                      : alert
                        ? gap > 0
                          ? "Gagne plus souvent que prévu : cotes trop généreuses ?"
                          : "Gagne moins souvent que prévu : cotes trop sévères ?"
                        : "Conforme aux cotes."}{" "}
                    Joueurs : <span className={cn("font-mono", b.playersNet >= 0 ? "text-success" : "text-destructive")}>{b.playersNet > 0 ? "+" : ""}{b.playersNet.toLocaleString("fr-FR")} pts</span>
                  </p>
                </div>
              );
            })}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
