/**
 * Anecdotes courtes et originales sur le basket/la NBA, rédigées pour
 * HoopPicks : pas de citation ni de contenu copié d'un article existant.
 */
export const BASKETBALL_FACTS: string[] = [
  "Le panier de basket a été inventé en 1891 avec... un vrai panier de pêches cloué à une balustrade.",
  "Le terrain NBA mesure 28,65 m de long, soit presque la longueur exacte d'un terrain de tennis.",
  "La ligne à 3 points n'existe dans la NBA que depuis la saison 1979-80.",
  "Le record de points en un seul match NBA est de 100, signé Wilt Chamberlain en 1962.",
  "Un ballon de basket officiel NBA compte exactement 8 panneaux de cuir.",
  "La violation des 24 secondes au tir a été introduite en 1954 pour dynamiser le jeu.",
  "Le terme \"dunk\" seul ne suffit pas à décrire un smash à deux mains devant un défenseur : ça, c'est un poster.",
  "Un match NBA dure 48 minutes de jeu effectif, réparties en 4 quart-temps de 12 minutes.",
  "La ligne de lancer-franc est à 4,57 m du panier, peu importe la catégorie ou le niveau.",
  "Le premier match de basket professionnel s'est joué en 1946, bien avant la création officielle de la NBA.",
  "Un joueur qui rate ses 6 premiers tirs mais inscrit le tir décisif reste, statistiquement, à moins de 50% de réussite. Le money time ne triche pas avec les stats.",
  "La NBA compte 30 franchises, réparties en deux conférences de 15 équipes chacune.",
];

export function pickRandomFact(exclude?: string): string {
  const pool = exclude ? BASKETBALL_FACTS.filter((f) => f !== exclude) : BASKETBALL_FACTS;
  return pool[Math.floor(Math.random() * pool.length)] ?? BASKETBALL_FACTS[0];
}
