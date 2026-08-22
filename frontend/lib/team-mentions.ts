/**
 * Détection best-effort d'une équipe mentionnée dans un titre d'actu — les
 * flux d'actus utilisent des surnoms ("Cavs", "Wolves", "Sixers"), pas les
 * abréviations officielles de team-colors.ts, d'où cette table à part.
 * Sert uniquement à choisir un repère couleur ; un faux positif occasionnel
 * (ex. "Magic" hors contexte NBA) n'a pas de conséquence fonctionnelle.
 */
const TEAM_MENTION_PATTERNS: [RegExp, string][] = [
  [/\bhawks\b/i, "ATL"],
  [/\bceltics\b/i, "BOS"],
  [/\bnets\b/i, "BKN"],
  [/\bhornets\b/i, "CHA"],
  [/\bbulls\b/i, "CHI"],
  [/\bcavaliers\b|\bcavs\b/i, "CLE"],
  [/\bmavericks\b|\bmavs\b/i, "DAL"],
  [/\bnuggets\b/i, "DEN"],
  [/\bpistons\b/i, "DET"],
  [/\bwarriors\b/i, "GSW"],
  [/\brockets\b/i, "HOU"],
  [/\bpacers\b/i, "IND"],
  [/\bclippers\b|\bclips\b/i, "LAC"],
  [/\blakers\b/i, "LAL"],
  [/\bgrizzlies\b/i, "MEM"],
  [/\bheat\b/i, "MIA"],
  [/\bbucks\b/i, "MIL"],
  [/\btimberwolves\b|\bwolves\b/i, "MIN"],
  [/\bpelicans\b/i, "NOP"],
  [/\bknicks\b/i, "NYK"],
  [/\bthunder\b/i, "OKC"],
  [/\bmagic\b/i, "ORL"],
  [/\b76ers\b|\bsixers\b/i, "PHI"],
  [/\bsuns\b/i, "PHX"],
  [/\btrail blazers\b|\bblazers\b/i, "POR"],
  [/\bkings\b/i, "SAC"],
  [/\bspurs\b/i, "SAS"],
  [/\braptors\b/i, "TOR"],
  [/\bjazz\b/i, "UTA"],
  [/\bwizards\b/i, "WAS"],
];

export function detectTeamMention(title: string): string | null {
  for (const [pattern, abbreviation] of TEAM_MENTION_PATTERNS) {
    if (pattern.test(title)) return abbreviation;
  }
  return null;
}
