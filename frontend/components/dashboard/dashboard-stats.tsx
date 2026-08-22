"use client";

import { Wallet, Target, Trophy, Ticket } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

interface StatItem {
  label: string;
  value?: string;
  hint?: string;
  icon: React.ElementType;
  isLoading: boolean;
}

export function DashboardStats({ items }: { items: StatItem[] }) {
  return (
    <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
      {items.map((item) => (
        <Card key={item.label}>
          <CardContent className="flex items-start justify-between gap-3 pt-6">
            <div>
              <p className="text-xs uppercase tracking-wide text-muted-foreground">
                {item.label}
              </p>
              {item.isLoading ? (
                <Skeleton className="mt-2 h-7 w-20" />
              ) : (
                <p className="mt-1 font-heading text-2xl font-bold">
                  {item.value ?? "—"}
                </p>
              )}
              {!item.isLoading && item.hint && (
                <p className="mt-0.5 text-xs text-muted-foreground">{item.hint}</p>
              )}
            </div>
            <div
              className={cn(
                "flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary"
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
