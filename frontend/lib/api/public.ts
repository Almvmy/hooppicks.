import { LeaderboardEntry, Match } from "@/lib/types";

const BACKEND_URL = process.env.BACKEND_API_URL ?? "http://localhost:3001";

/**
 * Pour les Server Components uniquement : appelle le backend directement
 * (pas de session à transmettre, ces routes sont publiques), avec un cache
 * ISR léger. En cas d'échec, renvoie `undefined` plutôt que de faire planter
 * la page : ces données ne sont qu'un bonus d'accueil, pas un contenu
 * critique.
 */
async function fetchPublic<T>(path: string): Promise<T | undefined> {
  try {
    const res = await fetch(`${BACKEND_URL}${path}`, {
      next: { revalidate: 60 },
    });
    if (!res.ok) return undefined;
    return (await res.json()) as T;
  } catch {
    return undefined;
  }
}

export function fetchPublicLeaderboard() {
  return fetchPublic<LeaderboardEntry[]>("/leaderboard");
}

export function fetchPublicMatches() {
  return fetchPublic<Match[]>("/matches");
}
