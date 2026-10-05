import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"
import { resolveTimeZone } from "@/lib/time-zone"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

// Toutes les dates affichées passent par ces helpers avec le fuseau choisi
// (lib/time-zone.ts) : paramètre explicite pour les composants réactifs
// (useTimeZone), sinon résolu à l'appel.

/** Jour calendaire (aaaa-mm-jj) d'un instant dans un fuseau donné. */
function calendarDay(date: Date, timeZone: string): number {
  const parts = new Intl.DateTimeFormat("en-CA", { timeZone, year: "numeric", month: "2-digit", day: "2-digit" })
    .format(date);
  return Date.parse(`${parts}T00:00:00Z`);
}

/** "Aujourd'hui", "Demain", "Hier", ou la date formatée sinon : pour grouper une liste par jour. */
export function getDayLabel(date: Date, timeZone = resolveTimeZone()): string {
  const diffDays = Math.round((calendarDay(date, timeZone) - calendarDay(new Date(), timeZone)) / 86400000);

  if (diffDays === 0) return "Aujourd'hui";
  if (diffDays === 1) return "Demain";
  if (diffDays === -1) return "Hier";
  return date.toLocaleDateString("fr-FR", { weekday: "long", day: "2-digit", month: "long", timeZone });
}

/** "22 août" : date courte d'un match, pour les cartes/lignes de calendrier. */
export function formatMatchDate(date: Date, timeZone = resolveTimeZone()): string {
  return date.toLocaleDateString("fr-FR", { day: "2-digit", month: "short", timeZone });
}

/** "20:30" : heure d'un match, pour les cartes/lignes de calendrier. */
export function formatMatchTime(date: Date, timeZone = resolveTimeZone()): string {
  return date.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit", timeZone });
}

/** "05 oct., 14:30" : horodatage court (notifications, synchro admin). */
export function formatShortDateTime(date: Date, timeZone = resolveTimeZone()): string {
  return date.toLocaleDateString("fr-FR", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit", timeZone });
}

/** "05 oct. 2026" : date avec année (historique des paris et transactions). */
export function formatLongDate(date: Date, timeZone = resolveTimeZone()): string {
  return date.toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric", timeZone });
}

/**
 * "Commence dans 45 min" pour un match SCHEDULED proche du coup d'envoi :
 * null au-delà de 3h (pas d'urgence à créer artificiellement) ou une fois le
 * match démarré (le badge de statut suffit alors). Volontairement discret :
 * juste de quoi inciter à décider maintenant plutôt qu'oublier, pas un
 * décompte seconde par seconde.
 */
export function formatKickoffCountdown(iso: string): string | null {
  const diffMinutes = Math.round((new Date(iso).getTime() - Date.now()) / 60000);
  if (diffMinutes <= 0 || diffMinutes > 180) return null;
  if (diffMinutes < 60) return `Commence dans ${diffMinutes} min`;
  const hours = Math.floor(diffMinutes / 60);
  const minutes = diffMinutes % 60;
  return `Commence dans ${hours} h${minutes > 0 ? ` ${minutes}` : ""}`;
}

/** "il y a 2j", "à l'instant"... pour les fils d'activité. */
export function formatRelativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const diffMinutes = Math.floor(diffMs / 60000);

  if (diffMinutes < 1) return "à l'instant";
  if (diffMinutes < 60) return `il y a ${diffMinutes} min`;
  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `il y a ${diffHours} h`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return `il y a ${diffDays} j`;
  const diffWeeks = Math.floor(diffDays / 7);
  return `il y a ${diffWeeks} sem.`;
}
