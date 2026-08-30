/**
 * Traduit une position au classement en titre évocateur plutôt qu'un
 * simple numéro : gamifie la progression sans ajouter de donnée backend.
 */
export function rankTitle(rank: number | undefined, totalPlayers: number | undefined): string {
  if (!rank || !totalPlayers || totalPlayers === 0) return "Rookie";
  if (rank === 1) return "MVP";

  const percentile = rank / totalPlayers;
  if (percentile <= 0.05) return "All-Star";
  if (percentile <= 0.2) return "Starter";
  if (percentile <= 0.5) return "Rotation";
  return "Rookie";
}
