import { useSyncExternalStore } from "react";
import { detectTeamMention } from "@/lib/team-mentions";
import { Match, NewsItem } from "@/lib/types";

/**
 * Équipes d'un article : celles étiquetées par ESPN (API JSON) ; en repli
 * sur le flux RSS, qui n'en donne aucune, la détection par mot-clé du titre.
 */
export function teamsOf(item: NewsItem): string[] {
  if (item.teams?.length) return item.teams;
  const mentioned = detectTeamMention(item.title);
  return mentioned ? [mentioned] : [];
}

// Au-delà, un « prochain match » n'aide plus à décider d'un pari maintenant.
const NEXT_MATCH_HORIZON_MS = 3 * 24 * 60 * 60 * 1000;

/** Prochain match encore ouvert aux paris de chaque équipe, dans les 3 jours. */
export function nextMatchByTeam(matches: Match[] | undefined, now: number): Map<string, Match> {
  const result = new Map<string, Match>();
  const upcoming = (matches ?? [])
    .filter((m) => {
      const t = new Date(m.date).getTime();
      return m.status === "scheduled" && t > now && t - now < NEXT_MATCH_HORIZON_MS;
    })
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
  for (const m of upcoming) {
    for (const abbr of [m.homeTeam.abbreviation, m.awayTeam.abbreviation]) {
      if (!result.has(abbr)) result.set(abbr, m);
    }
  }
  return result;
}

/* ── « Nouveau depuis ta dernière visite » ───────────────────────────── */

const VISITS_KEY = "hp:news:visits";
// Pages vues à moins de 30 min d'écart = une même visite : sinon un simple
// aller-retour vers un article effacerait tous les marqueurs « Nouveau ».
const SAME_VISIT_MS = 30 * 60 * 1000;

interface Visits {
  previous: number | null; // début de la visite précédente
  last: number; // dernière page vue
}

function readVisits(): Visits | null {
  try {
    const raw = localStorage.getItem(VISITS_KEY);
    return raw ? (JSON.parse(raw) as Visits) : null;
  } catch {
    return null;
  }
}

/** Seuil « nouveau » : articles publiés après la visite précédente (null à la toute première visite). */
function newsThreshold(): number | null {
  const visits = readVisits();
  if (!visits) return null;
  return Date.now() - visits.last > SAME_VISIT_MS ? visits.last : visits.previous;
}

const noopSubscribe = () => () => {};

export function useNewsThreshold(): number | null {
  // Lu au rendu côté client seulement (localStorage n'existe pas au rendu
  // serveur) : null au premier rendu, donc pas d'écart d'hydratation.
  return useSyncExternalStore(noopSubscribe, newsThreshold, () => null);
}

/** À appeler une fois le fil affiché : enregistre la visite en cours. */
export function recordNewsVisit() {
  const now = Date.now();
  const visits = readVisits();
  const next: Visits = !visits
    ? { previous: null, last: now }
    : now - visits.last > SAME_VISIT_MS
      ? { previous: visits.last, last: now }
      : { previous: visits.previous, last: now };
  try {
    localStorage.setItem(VISITS_KEY, JSON.stringify(next));
  } catch {
    // Stockage indisponible (navigation privée…) : pas de marqueur, sans plus.
  }
}
