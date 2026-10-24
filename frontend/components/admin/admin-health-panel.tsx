"use client";

import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, CheckCircle2, Clock, HelpCircle } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import { fetchAdminHealth, JobHealth } from "@/lib/api/admin";
import { cn, formatRelativeTime } from "@/lib/utils";

const STATUS: Record<JobHealth["status"], { label: string; icon: React.ElementType; className: string }> = {
  OK: { label: "OK", icon: CheckCircle2, className: "text-success" },
  LATE: { label: "En retard", icon: Clock, className: "text-amber-500" },
  FAILING: { label: "En échec", icon: AlertTriangle, className: "text-destructive" },
  NEVER: { label: "Jamais lancée", icon: HelpCircle, className: "text-muted-foreground" },
};

// Problèmes d'abord : c'est pour eux qu'on ouvre cet onglet.
const ORDER: JobHealth["status"][] = ["FAILING", "LATE", "NEVER", "OK"];

function every(minutes: number) {
  if (minutes === 1) return "chaque minute";
  return minutes >= 1440 ? "chaque jour" : minutes >= 60 ? `toutes les ${minutes / 60} h` : `toutes les ${minutes} min`;
}

/** Santé des tâches de fond : dernier succès (et son résultat), dernier échec. */
export function AdminHealthPanel() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin", "health"],
    queryFn: fetchAdminHealth,
    refetchInterval: 60 * 1000,
  });

  if (isLoading) return <BasketballLoader label="Chargement..." />;
  if (isError || !data) return <p className="text-sm text-destructive">Impossible de charger la santé des tâches.</p>;

  const jobs = [...data].sort((a, b) => ORDER.indexOf(a.status) - ORDER.indexOf(b.status));
  return (
    <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
      {jobs.map((j) => {
        const s = STATUS[j.status];
        const Icon = s.icon;
        return (
          <Card key={j.key} className="h-full">
            <CardContent className="flex flex-col gap-1.5">
              <div className="flex items-center justify-between gap-2">
                <p className="font-semibold">{j.label}</p>
                <span className={cn("flex shrink-0 items-center gap-1 text-xs font-semibold", s.className)}>
                  <Icon className="h-3.5 w-3.5" />
                  {s.label}
                </span>
              </div>
              <p className="text-xs text-muted-foreground">
                {every(j.everyMinutes)}
                {j.lastSuccessAt && ` · réussie ${formatRelativeTime(j.lastSuccessAt)}`}
                {j.lastDetail && ` · ${j.lastDetail}`}
              </p>
              {j.lastFailureAt && (
                <p className={cn("break-words text-xs", j.status === "FAILING" ? "text-destructive" : "text-muted-foreground")}>
                  Échec {formatRelativeTime(j.lastFailureAt)}
                  {j.lastError && ` : ${j.lastError}`}
                </p>
              )}
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
