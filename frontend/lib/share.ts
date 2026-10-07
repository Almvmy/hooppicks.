// Liens de partage (aperçu WhatsApp & co.) : lus côté serveur, sans session,
// sur les routes publiques /share du backend.

const BACKEND_URL = process.env.BACKEND_API_URL ?? "http://localhost:3001";

/** Adresse publique du site, pour les liens et les images d'aperçu (absolues). */
export const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "https://hooppicks.vercel.app";

export interface SharedSelection {
  label: string;
  matchLabel: string | null;
  odds: number;
}

export interface SharedTicket {
  username: string;
  avatarNumber: number;
  avatarColorway: string;
  status: "won" | "lost" | "void";
  stake: number;
  totalOdds: number;
  potentialPayout: number;
  selections: SharedSelection[];
  resolvedAt: string | null;
}

export interface SharedPlayer {
  username: string;
  avatarNumber: number;
  favoriteTeam: string | null;
  seasonPoints: number;
  seasonRank: number | null;
  seasonPlayers: number;
  winRate: number;
  settledTickets: number;
  bestStreak: number;
  weekPoints: number;
  weekRank: number | null;
  weekPlayers: number;
  currentStreak: number;
}

async function get<T>(path: string, fresh = false): Promise<T | null> {
  try {
    // Court cache : l'aperçu est relu par chaque appli qui affiche le lien.
    // `fresh` pour la carte qu'on vient de demander à partager : chiffres du moment.
    const res = await fetch(`${BACKEND_URL}${path}`, fresh ? { cache: "no-store" } : { next: { revalidate: 300 } });
    return res.ok ? ((await res.json()) as T) : null;
  } catch {
    return null;
  }
}

export const fetchSharedTicket = (id: string) => get<SharedTicket>(`/share/tickets/${encodeURIComponent(id)}`);
export const fetchSharedPlayer = (username: string, fresh = false) =>
  get<SharedPlayer>(`/share/players/${encodeURIComponent(username)}`, fresh);

/**
 * « 1 475 » avec une espace ordinaire : l'espace fine de fr-FR n'existe pas
 * dans la police des images d'aperçu et s'afficherait en carré vide.
 */
export function plainNumber(n: number): string {
  return Math.abs(Math.round(n)).toString().replace(/\B(?=(\d{3})+(?!\d))/g, " ");
}

export function signedPlain(n: number): string {
  return `${n > 0 ? "+" : n < 0 ? "−" : ""}${plainNumber(n)}`;
}
