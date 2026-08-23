import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/** "Aujourd'hui", "Demain", "Hier", ou la date formatée sinon — pour grouper une liste par jour. */
export function getDayLabel(date: Date): string {
  const startOfDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  const today = startOfDay(new Date());
  const target = startOfDay(date);
  const diffDays = Math.round((target - today) / 86400000);

  if (diffDays === 0) return "Aujourd'hui";
  if (diffDays === 1) return "Demain";
  if (diffDays === -1) return "Hier";
  return date.toLocaleDateString("fr-FR", { weekday: "long", day: "2-digit", month: "long" });
}

/** "22 août" — date courte d'un match, pour les cartes/lignes de calendrier. */
export function formatMatchDate(date: Date): string {
  return date.toLocaleDateString("fr-FR", { day: "2-digit", month: "short" });
}

/** "20:30" — heure d'un match, pour les cartes/lignes de calendrier. */
export function formatMatchTime(date: Date): string {
  return date.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
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
