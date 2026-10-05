"use client";

import { useSyncExternalStore } from "react";

// Horloge partagée à la seconde. useSyncExternalStore plutôt qu'un
// setInterval + setState : le serveur rend "--" (getServerSnapshot = null)
// et le client prend la main sans écart d'hydratation.
function subscribe(onTick: () => void) {
  const id = setInterval(onTick, 1000);
  return () => clearInterval(id);
}
const getNow = () => Math.floor(Date.now() / 1000);
const getServerNow = () => null;

const UNITS = [
  { label: "jours", seconds: 86400 },
  { label: "heures", seconds: 3600 },
  { label: "min", seconds: 60 },
  { label: "sec", seconds: 1 },
] as const;

// Découpe des secondes restantes en jours/heures/min/sec (chaque unité prend
// ce qui reste après les unités plus grandes).
function splitDuration(totalSeconds: number): number[] {
  return UNITS.map(({ seconds }, i) => {
    const larger = i === 0 ? Infinity : UNITS[i - 1].seconds;
    return Math.floor((totalSeconds % larger) / seconds);
  });
}

/**
 * Compte à rebours vers le prochain match réellement programmé (lu en base),
 * pas une date de saison écrite en dur : il reste juste toute l'année, en
 * intersaison comme en pleine saison.
 */
export function KickoffCountdown({ target }: { target: string }) {
  const now = useSyncExternalStore(subscribe, getNow, getServerNow);
  const targetSeconds = Math.floor(new Date(target).getTime() / 1000);
  const remaining = now === null ? null : Math.max(targetSeconds - now, 0);

  const parts = remaining === null ? null : splitDuration(remaining);

  if (remaining === 0) {
    return (
      <p className="font-heading text-lg font-bold text-primary">
        C&apos;est l&apos;heure du coup d&apos;envoi !
      </p>
    );
  }

  return (
    <div className="grid grid-cols-4 gap-2 sm:gap-3" role="timer" aria-live="off">
      {UNITS.map(({ label }, i) => {
        const value = parts ? parts[i] : null;
        return (
          <div key={label} className="glass-inset-quiet flex flex-col items-center rounded-xl px-2 py-3">
            <span className="font-mono text-2xl font-bold tabular-nums text-foreground sm:text-3xl">
              {value === null ? "--" : String(value).padStart(2, "0")}
            </span>
            <span className="mt-1 text-[10px] uppercase tracking-widest text-muted-foreground">
              {label}
            </span>
          </div>
        );
      })}
    </div>
  );
}
