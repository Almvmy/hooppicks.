import { UserBadge } from "@/lib/types";
import { fetchProfile } from "@/lib/api/auth";

// TEMPORAIRE : sera remplacé par fetch("/api/users/me/badges").
// Le calcul de déblocage (seuils, conditions) vivra côté backend NestJS ;
// le frontend se contente d'afficher le résultat déjà calculé, comme ici.
export async function fetchBadges(): Promise<UserBadge[]> {
  await new Promise((r) => setTimeout(r, 400));
  const profile = await fetchProfile();

  return [
    { id: "first-bet", label: "Premier pari", description: "Place ton tout premier ticket", unlocked: profile.totalBets >= 1 },
    { id: "ten-bets", label: "Habitué", description: "Place 10 paris", unlocked: profile.totalBets >= 10 },
    { id: "twenty-five-bets", label: "Vétéran", description: "Place 25 paris", unlocked: profile.totalBets >= 25 },
    { id: "sharp-shooter", label: "Sharpshooter", description: "Atteins 55% de réussite", unlocked: profile.winRate >= 55 },
    { id: "hot-streak", label: "Série chaude", description: "Atteins 65% de réussite", unlocked: profile.winRate >= 65 },
  ];
}