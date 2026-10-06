"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Card, CardContent } from "@/components/ui/card";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import { fetchAdminAudit } from "@/lib/api/admin";
import { cn, formatShortDateTime } from "@/lib/utils";

type Category = "all" | "points" | "users" | "matches" | "sync" | "announce";

// Codes renvoyés par le backend (AdminConsoleController / AdminActionsService).
export const AUDIT_ACTIONS: Record<string, { label: string; category: Exclude<Category, "all">; sensitive?: boolean }> = {
  SYNC_TEAMS: { label: "Synchro des équipes", category: "sync" },
  SYNC_GAMES: { label: "Synchro des matchs", category: "sync" },
  SYNC_ROSTERS: { label: "Synchro des effectifs", category: "sync" },
  SYNC_STANDINGS: { label: "Synchro du classement", category: "sync" },
  SYNC_PLAYER_STATS: { label: "Lot de stats joueurs", category: "sync" },
  RESOLVE_BETS: { label: "Résolution des paris", category: "points" },
  VOID_BET: { label: "Pari annulé et remboursé", category: "points", sensitive: true },
  VOID_MATCH_BETS: { label: "Paris d'un match annulés", category: "points", sensitive: true },
  ADJUST_WALLET: { label: "Solde ajusté", category: "points", sensitive: true },
  PROMOTE_ADMIN: { label: "Promu admin", category: "users", sensitive: true },
  DEMOTE_ADMIN: { label: "Droits admin retirés", category: "users", sensitive: true },
  DELETE_USER: { label: "Compte supprimé", category: "users", sensitive: true },
  UPDATE_MATCH: { label: "Match corrigé", category: "matches", sensitive: true },
  UNLOCK_MATCH: { label: "Match déverrouillé", category: "matches" },
  ANNOUNCE: { label: "Annonce envoyée", category: "announce" },
};

const CATEGORIES: { value: Category; label: string }[] = [
  { value: "all", label: "Tout" },
  { value: "points", label: "Points" },
  { value: "users", label: "Utilisateurs" },
  { value: "matches", label: "Matchs" },
  { value: "sync", label: "Synchros" },
  { value: "announce", label: "Annonces" },
];

export function AdminAuditLog() {
  const [category, setCategory] = useState<Category>("all");
  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin-audit"],
    queryFn: () => fetchAdminAudit(200),
  });

  const entries = (data ?? []).filter((e) => category === "all" || AUDIT_ACTIONS[e.action]?.category === category);

  return (
    <Card>
      <CardContent className="flex flex-col gap-4 pt-6">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="text-sm text-muted-foreground">Qui a fait quoi depuis la console, les 200 dernières actions.</p>
          <div className="flex flex-wrap gap-1.5" role="group" aria-label="Filtrer le journal">
            {CATEGORIES.map((c) => (
              <button
                key={c.value}
                type="button"
                onClick={() => setCategory(c.value)}
                className={cn(
                  "rounded-full px-3 py-1 text-xs transition-colors",
                  category === c.value ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
                )}
              >
                {c.label}
              </button>
            ))}
          </div>
        </div>

        {isLoading && <BasketballLoader label="Chargement du journal..." />}
        {isError && <p className="text-sm text-destructive">Impossible de charger le journal.</p>}
        {data && entries.length === 0 && <p className="text-sm text-muted-foreground">Aucune action enregistrée.</p>}

        <ol className="flex flex-col">
          {entries.map((e) => {
            const meta = AUDIT_ACTIONS[e.action];
            return (
              <li key={e.id} className="flex flex-wrap items-baseline gap-x-3 gap-y-0.5 py-2.5 shadow-[inset_0_-1px_0_var(--hairline)] last:shadow-none">
                <span className="w-32 shrink-0 font-mono text-xs text-muted-foreground">{formatShortDateTime(new Date(e.date))}</span>
                <span className="text-sm">
                  <strong>@{e.adminUsername}</strong>{" "}
                  <span className={cn(meta?.sensitive && "font-semibold text-primary")}>{meta?.label ?? e.action}</span>
                  {e.target && <span className="text-muted-foreground"> · {e.target}</span>}
                </span>
                {e.details && <span className="basis-full pl-0 text-xs text-muted-foreground sm:pl-[8.75rem]">{e.details}</span>}
              </li>
            );
          })}
        </ol>
      </CardContent>
    </Card>
  );
}
