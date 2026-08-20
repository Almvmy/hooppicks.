"use client";

import Image from "next/image";
import Link from "next/link";
import { Flame, ArrowRight } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { greetingForNow } from "@/lib/dashboard";
import { CourtWatermark } from "@/components/court-watermark";

interface DashboardHeroProps {
  username?: string;
  isLoading: boolean;
  streak: number;
  pendingCount: number;
}

function subtitleFor(streak: number, pendingCount: number): string {
  if (pendingCount > 0) {
    return `${pendingCount} ticket${pendingCount > 1 ? "s" : ""} en attente de résultat.`;
  }
  if (streak >= 3) {
    return `${streak} pronostics gagnés d'affilée. Reste chaud.`;
  }
  if (streak > 0) {
    return `${streak} pronostic${streak > 1 ? "s" : ""} gagné${streak > 1 ? "s" : ""} d'affilée.`;
  }
  return "Prêt à lancer un nouveau pronostic ?";
}

export function DashboardHero({
  username,
  isLoading,
  streak,
  pendingCount,
}: DashboardHeroProps) {
  return (
    <div className="relative overflow-hidden rounded-xl border border-border bg-card">
      <Image
        src="/images/hoop-net.jpg"
        alt=""
        fill
        sizes="(min-width: 640px) 100vw, 100vw"
        className="object-cover opacity-[0.08]"
      />
      <div className="absolute inset-0 bg-gradient-to-r from-card via-card/95 to-card/70" />
      {/* Signature : tracé de terrain de basket en filigrane, ancré à droite */}
      <CourtWatermark />

      <div className="relative flex flex-col gap-4 p-6 sm:flex-row sm:items-center sm:justify-between">
        <div>
          {isLoading ? (
            <Skeleton className="h-8 w-56" />
          ) : (
            <h1 className="font-heading text-2xl font-bold">
              {greetingForNow()}, {username ?? "champion"}
            </h1>
          )}
          {isLoading ? ( 
            <Skeleton className="mt-2 h-4 w-64" />
          ) : ( 
            <p className="mt-1.5 text-sm text-muted-foreground">
            {subtitleFor(streak, pendingCount)} + </p>
            )}


        </div>

        <div className="flex items-center gap-3">
          {!isLoading && streak > 0 && (
            <div
              className={cn(
                "flex items-center gap-2 rounded-full border px-3 py-1.5",
                streak >= 3
                  ? "border-primary/40 bg-primary/10 text-primary"
                  : "border-border bg-secondary/50 text-foreground"
              )}
            >
              <Flame
                className={cn(
                  "h-4 w-4",
                  streak >= 3 ? "text-primary" : "text-muted-foreground"
                )}
              />
              <span className="font-mono text-sm font-bold">{streak}</span>
              <span className="text-xs text-muted-foreground">série</span>
            </div>
          )}

          <Link
            href="/matches"
            className={cn(buttonVariants({ variant: "default" }), "gap-1.5")}
          >
            Voir les matchs
            <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
      </div>
    </div>
  );
}
