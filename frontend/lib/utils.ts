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

/**
 * "2026-27" : une saison NBA démarre en octobre, donc un match de janvier
 * 2027 appartient à la saison 2026-27. Déduit d'une date plutôt qu'écrit en dur.
 */
export function seasonLabel(reference: Date = new Date()): string {
  const start = reference.getMonth() >= 8 ? reference.getFullYear() : reference.getFullYear() - 1;
  return `${start}-${String(start + 1).slice(2)}`;
}

/** "2026-10-21" : jour calendaire d'un instant dans le fuseau d'affichage (pour grouper/filtrer par jour). */
export function dayKey(date: Date, timeZone = resolveTimeZone()): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone, year: "numeric", month: "2-digit", day: "2-digit" }).format(date);
}

/**
 * Libellé en deux lignes pour une pastille de date : "Auj." / "21 oct.",
 * "mar." / "21 oct.". Relatif pour hier/aujourd'hui/demain, sinon jour court.
 */
export function formatDayChip(date: Date, timeZone = resolveTimeZone()): { top: string; bottom: string } {
  const label = getDayLabel(date, timeZone);
  const top =
    label === "Aujourd'hui" ? "Auj." : label === "Demain" ? "Demain" : label === "Hier" ? "Hier"
      : date.toLocaleDateString("fr-FR", { weekday: "short", timeZone });
  return { top, bottom: date.toLocaleDateString("fr-FR", { day: "numeric", month: "short", timeZone }) };
}

// --- Semaines de jeu (cf. BankrollService côté backend) ---
// Chaque lundi à 12h GMT, tout le monde repart avec ce solde.
export const WEEKLY_BANKROLL = 1000;
const BANKROLL_ZONE = "UTC";

// Décalage du fuseau du jeu sur UTC à cet instant (nul en GMT, mais calculé
// pour rester juste si le fuseau du jeu change un jour).
function parisOffsetMs(at: Date): number {
  const p = Object.fromEntries(
    new Intl.DateTimeFormat("en-US", {
      timeZone: BANKROLL_ZONE,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hourCycle: "h23",
    })
      .formatToParts(at)
      .map((part) => [part.type, part.value])
  );
  const wall = Date.UTC(+p.year, +p.month - 1, +p.day, +p.hour, +p.minute, +p.second);
  return wall - Math.floor(at.getTime() / 1000) * 1000;
}

/** Prochain passage à une nouvelle semaine de jeu : lundi 12h GMT. */
export function nextBankrollReset(now: Date = new Date()): Date {
  const wall = new Date(now.getTime() + parisOffsetMs(now));
  const candidate = new Date(Date.UTC(wall.getUTCFullYear(), wall.getUTCMonth(), wall.getUTCDate(), 12));
  candidate.setUTCDate(candidate.getUTCDate() + ((8 - candidate.getUTCDay()) % 7));
  if (candidate.getTime() <= wall.getTime()) candidate.setUTCDate(candidate.getUTCDate() + 7);
  // Décalage recalculé à la date visée : un changement d'heure peut tomber entre les deux.
  const approx = new Date(candidate.getTime() - parisOffsetMs(now));
  return new Date(candidate.getTime() - parisOffsetMs(approx));
}

/** « lundi 12h (dans 3 j) » / « lundi 12h (dans 5 h) ». */
export function formatBankrollReset(now: Date = new Date()): string {
  const hours = (nextBankrollReset(now).getTime() - now.getTime()) / 3_600_000;
  const delay = hours < 24 ? `dans ${Math.max(1, Math.round(hours))} h` : `dans ${Math.round(hours / 24)} j`;
  return `lundi 12h (${delay})`;
}

/** Bénéfice net signé : « +1 240 pts », « −350 pts », « 0 pts ». */
export function formatSignedPoints(points: number, unit = true): string {
  const abs = Math.abs(points).toLocaleString("fr-FR");
  const sign = points > 0 ? "+" : points < 0 ? "−" : "";
  return `${sign}${abs}${unit ? " pts" : ""}`;
}
