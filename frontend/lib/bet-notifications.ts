const STORAGE_KEY = "hooppicks:celebrated-bet-ids";

function readCelebratedIds(): Set<string> {
  if (typeof window === "undefined") return new Set();
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    return raw ? new Set(JSON.parse(raw) as string[]) : new Set();
  } catch {
    return new Set();
  }
}

function writeCelebratedIds(ids: Set<string>) {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify([...ids]));
  } catch {
    // stockage indisponible : on ignore silencieusement
  }
}

/**
 * Repère les tickets gagnants pas encore "fêtés" (statut "won" détecté
 * depuis la dernière visite) et marque tous les paris déjà résolus comme
 * vus, pour ne célébrer chaque victoire qu'une seule fois.
 */
export function detectNewlyWonBets<T extends { id: string; status: string }>(bets: T[]): T[] {
  const celebrated = readCelebratedIds();
  const resolved = bets.filter(
    (b) => b.status === "won" || b.status === "lost" || b.status === "void"
  );
  const newlyWon = bets.filter((b) => b.status === "won" && !celebrated.has(b.id));

  if (resolved.length > 0) {
    for (const bet of resolved) celebrated.add(bet.id);
    writeCelebratedIds(celebrated);
  }

  return newlyWon;
}
