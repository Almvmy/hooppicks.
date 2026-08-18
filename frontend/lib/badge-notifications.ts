const STORAGE_KEY = "hooppicks:seen-badge-ids";

function readSeenIds(): Set<string> {
  if (typeof window === "undefined") return new Set();
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    return raw ? new Set(JSON.parse(raw) as string[]) : new Set();
  } catch {
    return new Set();
  }
}

function writeSeenIds(ids: Set<string>) {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify([...ids]));
  } catch {
    // stockage indisponible (navigation privée, quota…) : on ignore silencieusement
  }
}

/**
 * Compare les badges débloqués à ceux déjà vus lors d'une précédente visite.
 * Renvoie la liste des badges nouvellement débloqués et met à jour le
 * stockage local en conséquence. Idempotent : appelée plusieurs fois avec le
 * même jeu de badges, elle ne renvoie de nouveaux badges qu'une seule fois.
 */
export function detectNewlyUnlockedBadges<T extends { id: string; unlocked: boolean }>(
  badges: T[]
): T[] {
  const seen = readSeenIds();
  const unlockedNow = badges.filter((b) => b.unlocked);
  const newlyUnlocked = unlockedNow.filter((b) => !seen.has(b.id));

  if (newlyUnlocked.length > 0) {
    for (const badge of unlockedNow) seen.add(badge.id);
    writeSeenIds(seen);
  }

  return newlyUnlocked;
}