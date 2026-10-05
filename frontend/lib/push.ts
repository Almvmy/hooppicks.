import { apiFetch } from "@/lib/api/http";
import { isIosDevice } from "@/lib/install-prompt";

/**
 * unsupported   : navigateur sans Web Push
 * needs-install : iPhone/iPad, le push n'existe que pour l'app ajoutée à l'écran d'accueil
 * unavailable   : serveur sans clés VAPID (push désactivé côté backend)
 * denied        : permission refusée, à rétablir dans les réglages du navigateur
 * disabled      : possible mais pas activé sur cet appareil
 * enabled       : cet appareil reçoit les notifications
 */
export type PushState = "unsupported" | "needs-install" | "unavailable" | "denied" | "disabled" | "enabled";

function isStandalone(): boolean {
  return (
    window.matchMedia("(display-mode: standalone)").matches ||
    (navigator as Navigator & { standalone?: boolean }).standalone === true
  );
}

function hasPushApis(): boolean {
  return "serviceWorker" in navigator && "PushManager" in window && "Notification" in window;
}

export function registerServiceWorker() {
  if (!("serviceWorker" in navigator)) return;
  navigator.serviceWorker.register("/sw.js").catch(() => {
    // Pas de service worker : seul le push est perdu, le reste de l'app marche.
  });
}

async function currentSubscription(): Promise<PushSubscription | null> {
  const registration = await navigator.serviceWorker.ready;
  return registration.pushManager.getSubscription();
}

export async function getPushState(): Promise<PushState> {
  // Testé avant les API : Safari iOS hors app installée n'expose même pas PushManager.
  if (isIosDevice() && !isStandalone()) return "needs-install";
  if (!hasPushApis()) return "unsupported";
  if (Notification.permission === "denied") return "denied";
  return (await currentSubscription()) ? "enabled" : "disabled";
}

// Clé VAPID base64url → octets, le format attendu par pushManager.subscribe.
function decodeKey(base64Url: string): Uint8Array<ArrayBuffer> {
  const base64 = (base64Url + "=".repeat((4 - (base64Url.length % 4)) % 4)).replace(/-/g, "+").replace(/_/g, "/");
  const binary = atob(base64);
  const bytes = new Uint8Array(new ArrayBuffer(binary.length));
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes;
}

export async function enablePush(): Promise<PushState> {
  let publicKey: string;
  try {
    ({ publicKey } = await apiFetch<{ publicKey: string }>("/push/public-key"));
  } catch {
    return "unavailable";
  }

  const permission = await Notification.requestPermission();
  if (permission !== "granted") return permission === "denied" ? "denied" : "disabled";

  const registration = await navigator.serviceWorker.ready;
  const subscription =
    (await registration.pushManager.getSubscription()) ??
    (await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: decodeKey(publicKey) }));

  await apiFetch<void>("/push/subscribe", { method: "POST", body: JSON.stringify(subscription.toJSON()) });
  return "enabled";
}

export async function disablePush(): Promise<PushState> {
  const subscription = await currentSubscription();
  if (subscription) {
    // Serveur d'abord : si l'appel échoue, l'abonnement navigateur reste
    // intact et l'état affiché ne ment pas.
    await apiFetch<void>("/push/unsubscribe", {
      method: "POST",
      body: JSON.stringify({ endpoint: subscription.endpoint }),
    });
    await subscription.unsubscribe();
  }
  return "disabled";
}

/**
 * À la déconnexion : sur un appareil partagé, le compte suivant ne doit pas
 * recevoir les notifications du précédent. Au mieux de l'effort, jamais
 * bloquant pour la déconnexion elle-même.
 */
export async function forgetPushOnThisDevice(): Promise<void> {
  try {
    if (!hasPushApis()) return;
    const registration = await navigator.serviceWorker.getRegistration();
    const subscription = await registration?.pushManager.getSubscription();
    if (!subscription) return;
    await apiFetch<void>("/push/unsubscribe", {
      method: "POST",
      body: JSON.stringify({ endpoint: subscription.endpoint }),
    }).catch(() => {});
    await subscription.unsubscribe();
  } catch {
    // Ignoré : la déconnexion passe avant tout.
  }
}
