import { toast } from "sonner";

/** Lien qui ouvre la page Ligues avec le code déjà saisi et la ligue affichée. */
export function leagueInviteUrl(inviteCode: string): string {
  return `${window.location.origin}/leagues?code=${encodeURIComponent(inviteCode)}`;
}

/**
 * Partage natif (WhatsApp, SMS… sur mobile) quand le navigateur le permet,
 * sinon copie du lien. Une annulation du partage par l'utilisateur n'est pas
 * une erreur.
 */
export async function shareLeagueInvite(league: { name: string; inviteCode: string }) {
  const url = leagueInviteUrl(league.inviteCode);
  const text = `Rejoins ma ligue « ${league.name} » sur HoopPicks (code ${league.inviteCode}) :`;
  if (navigator.share) {
    try {
      await navigator.share({ title: "HoopPicks", text, url });
      return;
    } catch (e) {
      if (e instanceof DOMException && e.name === "AbortError") return;
    }
  }
  try {
    await navigator.clipboard.writeText(`${text} ${url}`);
    toast.success("Lien d'invitation copié !");
  } catch {
    toast.error("Impossible de copier le lien.");
  }
}

export async function copyLeagueCode(inviteCode: string) {
  try {
    await navigator.clipboard.writeText(inviteCode);
    toast.success("Code copié !");
  } catch {
    toast.error("Impossible de copier le code.");
  }
}
