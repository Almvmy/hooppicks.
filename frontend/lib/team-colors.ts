/**
 * Couleur d'identité par équipe (abréviation NBA standard) — donnée factuelle
 * d'association couleur/équipe, comme les noms de franchise déjà listés dans
 * nba-teams.ts. Choisie pour rester lisible sur fond sombre plutôt que pour
 * matcher au pixel près une charte officielle (certaines équipes ont une
 * couleur principale trop sombre pour ressortir — on prend alors leur accent
 * le plus vif).
 */
export const TEAM_COLORS: Record<string, string> = {
  ATL: "#E03A3E",
  BOS: "#007A33",
  BKN: "#A1A1AA",
  CHA: "#00788C",
  CHI: "#CE1141",
  CLE: "#860038",
  DAL: "#00538C",
  DEN: "#FEC524",
  DET: "#C8102E",
  GSW: "#FFC72C",
  HOU: "#CE1141",
  IND: "#FDBB30",
  LAC: "#C8102E",
  LAL: "#FDB927",
  MEM: "#5D76A9",
  MIA: "#F9A01B",
  MIL: "#00471B",
  MIN: "#78BE20",
  NOP: "#B4975A",
  NYK: "#006BB6",
  OKC: "#007AC1",
  ORL: "#0077C0",
  PHI: "#ED174C",
  PHX: "#E56020",
  POR: "#E03A3E",
  SAC: "#5A2D81",
  SAS: "#C4CED4",
  TOR: "#CE1141",
  UTA: "#F9A01B",
  WAS: "#E31837",
};

export function getTeamColor(abbreviation: string): string {
  return TEAM_COLORS[abbreviation] ?? "#6B7280"; // gris neutre si équipe inconnue
}
