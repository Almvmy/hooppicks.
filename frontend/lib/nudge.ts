import { useCallback, useSyncExternalStore } from "react";

// Invitations (installer l'app, activer les notifications) : 3 fois au plus
// par appareil, une semaine d'écart au moins. Au-delà, ça devient du
// harcèlement et le joueur finit par tout refuser.
const MAX_SHOWS = 3;
const COOLDOWN_MS = 7 * 24 * 60 * 60 * 1000;

interface NudgeRecord {
  count: number;
  last: number;
}

const listeners = new Set<() => void>();
const memory = new Map<string, NudgeRecord>();

function read(key: string): NudgeRecord {
  try {
    const stored = window.localStorage.getItem(`nudge:${key}`);
    if (stored) return JSON.parse(stored) as NudgeRecord;
  } catch {
    // Stockage indisponible : on retombe sur la mémoire.
  }
  return memory.get(key) ?? { count: 0, last: 0 };
}

export function canNudge(key: string): boolean {
  if (typeof window === "undefined") return false;
  const { count, last } = read(key);
  return count < MAX_SHOWS && Date.now() - last > COOLDOWN_MS;
}

/** Une invitation montrée (toast) ou refermée (carte) : compte pour le plafond. */
export function recordNudge(key: string) {
  const next = { count: read(key).count + 1, last: Date.now() };
  memory.set(key, next);
  try {
    window.localStorage.setItem(`nudge:${key}`, JSON.stringify(next));
  } catch {
    // Stockage indisponible : l'invitation pourra revenir au rechargement.
  }
  listeners.forEach((l) => l());
}

export function useNudge(key: string): [boolean, () => void] {
  const subscribe = useCallback((onChange: () => void) => {
    listeners.add(onChange);
    return () => {
      listeners.delete(onChange);
    };
  }, []);
  const visible = useSyncExternalStore(subscribe, () => canNudge(key), () => false);
  const dismiss = useCallback(() => recordNudge(key), [key]);
  return [visible, dismiss];
}
