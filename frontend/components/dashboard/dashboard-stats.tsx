"use client";

import { Wallet, Target, Trophy, Ticket } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

interface StatItem {
  label: string;
  value?: React.ReactNode;
  hint?: string;
  icon: React.ElementType;
  /** "paint" : l'indigo d'appoint (classement, ligues), sinon l'orange. */
  tone?: "primary" | "paint";
  isLoading: boolean;
}

export function DashboardStats({ items }: { items: StatItem[] }) {
  return (
    // Compact sur mobile (cartes basses, sans icône, une ligne d'aide) : les
    // quatre cartes faisaient ~150 px chacune et occupaient tout le premier
    // écran du tableau de bord.
    <div className="stagger-children grid grid-cols-2 gap-2 sm:gap-4 lg:grid-cols-4">
      {items.map((item) => (
        <Card key={item.label} size="sm" className="sm:py-4">
          <CardContent className="flex items-start justify-between gap-3 sm:pt-2">
            <div className="min-w-0">
              <p className="truncate text-[11px] uppercase tracking-wide text-muted-foreground sm:text-xs">
                {item.label}
              </p>
              {item.isLoading ? (
                <Skeleton className="mt-2 h-6 w-20" />
              ) : (
                <p className="mt-0.5 whitespace-nowrap font-heading text-lg font-bold sm:mt-1 sm:text-2xl">
                  {item.value ?? "-"}
                </p>
              )}
              {!item.isLoading && item.hint && (
                <p className="truncate text-xs text-muted-foreground sm:mt-0.5 sm:whitespace-normal" title={item.hint}>
                  {item.hint}
                </p>
              )}
            </div>
            <div
              className={cn(
                "hidden h-9 w-9 shrink-0 items-center justify-center rounded-lg sm:flex",
                item.tone === "paint" ? "bg-paint/10 text-paint" : "bg-primary/10 text-primary"
              )}
            >
              <item.icon className="h-4.5 w-4.5" />
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

export const DASHBOARD_STAT_ICONS = { Wallet, Target, Trophy, Ticket };
