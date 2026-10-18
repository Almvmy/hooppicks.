"use client";

import { Flame, Sparkles } from "lucide-react";
import { countdownParts, launchPhase, useNow, useSeasonLaunch } from "@/lib/season-launch";

/**
 * Compte à rebours compact jusqu'au premier match de la saison, pour les
 * pages sans compte (connexion, inscription). Disparaît après la soirée d'ouverture.
 */
export function SeasonCountdownStrip() {
  const launch = useSeasonLaunch();
  const now = useNow();
  if (!launch) return null;
  const phase = launchPhase(launch, now);
  if (phase === "after") return null;
  const { days, hours, minutes, seconds } = countdownParts(new Date(launch.openerDate).getTime() - now);

  return (
    <div className="island mb-5 w-full max-w-sm overflow-hidden rounded-2xl bg-jersey px-4 py-3 text-jersey-foreground shadow-[var(--lift)]">
      <p className="flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-[0.16em] text-brand">
        <Sparkles className="h-3.5 w-3.5" />
        {phase === "before" ? `La saison ${launch.season} commence dans` : "C'est l'ouverture ce soir"}
      </p>
      {phase === "before" && (
        <div className="mt-2 grid grid-cols-4 gap-1.5" role="timer" aria-live="off">
          {[
            { v: days, l: "j" },
            { v: hours, l: "h" },
            { v: minutes, l: "min" },
            { v: seconds, l: "s" },
          ].map(({ v, l }) => (
            <div key={l} className="rounded-lg bg-white/[0.06] py-1.5 text-center ring-1 ring-white/10">
              <span className="font-mono text-lg font-bold tabular-nums">{String(v).padStart(2, "0")}</span>
              <span className="ml-0.5 text-[10px] font-semibold text-jersey-muted">{l}</span>
            </div>
          ))}
        </div>
      )}
      <p className="mt-2 flex items-center gap-1.5 text-xs text-jersey-foreground/85">
        <Flame className="h-3.5 w-3.5 shrink-0 text-brand" />
        Soirée d&apos;ouverture : toutes les cotes « vainqueur » boostées.
      </p>
    </div>
  );
}
