const STORAGE_KEY = "hooppicks:celebrated-bet-ids";

// null = rien d'enregistré sur cet appareil (première ouverture, app réinstallée).
function readCelebratedIds(): Set<string> | null {
  if (typeof window === "undefined") return new Set();
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    return raw ? new Set(JSON.parse(raw) as string[]) : null;
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
  const stored = readCelebratedIds();
  const resolved = bets.filter(
    (b) => b.status === "won" || b.status === "lost" || b.status === "void"
  );
  // Premier passage sur cet appareil : les tickets déjà réglés servent de
  // point de départ, sinon une réinstallation refêtait tous les anciens gains.
  if (stored === null) {
    writeCelebratedIds(new Set(resolved.map((b) => b.id)));
    return [];
  }
  const celebrated = stored;
  const newlyWon = bets.filter((b) => b.status === "won" && !celebrated.has(b.id));

  if (resolved.length > 0) {
    for (const bet of resolved) celebrated.add(bet.id);
    writeCelebratedIds(celebrated);
  }

  return newlyWon;
}
