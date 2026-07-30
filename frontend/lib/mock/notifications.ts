import { AppNotification } from "@/lib/types";

let notifications: AppNotification[] = [
  { id: "n1", type: "bet_won", message: "Ton pari sur Lakers (vainqueur) est gagnant : +160 pts", date: "2026-07-07T21:10:00Z", read: false },
  { id: "n2", type: "match_starting", message: "Warriors vs Nets commence dans 30 minutes", date: "2026-07-10T22:00:00Z", read: false },
  { id: "n3", type: "bet_lost", message: "Ton pari sur Nuggets vs Suns (total) n'est pas gagnant", date: "2026-07-08T23:30:00Z", read: true },
  { id: "n4", type: "system", message: "Bienvenue sur HoopPicks ! Tu as reçu 1000 pts de bonus.", date: "2026-07-01T10:00:00Z", read: true },
];

// TEMPORAIRE : sera remplacé par fetch("/api/notifications") + Socket.IO en semaine 7 pour le temps réel.
export async function fetchNotifications(): Promise<AppNotification[]> {
  await new Promise((r) => setTimeout(r, 400));
  return notifications;
}

export async function markNotificationRead(id: string): Promise<void> {
  await new Promise((r) => setTimeout(r, 200));
  // On recrée un nouveau tableau (et un nouvel objet pour l'élément modifié)
  // au lieu de muter en place — indispensable pour que TanStack Query détecte le changement.
  notifications = notifications.map((n) =>
    n.id === id ? { ...n, read: true } : n
  );
}