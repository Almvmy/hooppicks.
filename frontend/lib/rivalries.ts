/**
 * Grandes rivalités historiques de la NBA, par paire d'abréviations
 * d'équipe (ordre indifférent). Purement éditorial : sert uniquement à
 * mettre en valeur certains matchs sur le calendrier, aucune donnée
 * propriétaire ni logo impliqué.
 */
const RIVALRY_PAIRS: [string, string][] = [
  ["LAL", "BOS"],
  ["LAL", "LAC"],
  ["GSW", "CLE"],
  ["NYK", "BOS"],
  ["CHI", "DET"],
  ["MIA", "NYK"],
  ["DAL", "SAS"],
  ["OKC", "GSW"],
  ["PHI", "BOS"],
  ["DEN", "MIN"],
];

export function isRivalryMatchup(homeAbbr: string, awayAbbr: string): boolean {
  return RIVALRY_PAIRS.some(
    ([a, b]) =>
      (a === homeAbbr && b === awayAbbr) || (a === awayAbbr && b === homeAbbr)
  );
}
