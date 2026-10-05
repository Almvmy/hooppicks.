// État d'installation de l'app (PWA), partagé entre la capture globale
// (InstallPromptCapture, montée dans le layout racine) et les boutons
// "Installer" affichés plus loin dans l'app.

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>;
  userChoice: Promise<{ outcome: "accepted" | "dismissed" }>;
}

/**
 * installed   : déjà ouverte comme app installée
 * promptable  : Chrome/Edge/Android, installation en un clic possible
 * ios         : iPhone/iPad, installation manuelle via le menu Partager
 * unsupported : rien à proposer (Firefox desktop, rendu serveur...)
 */
export type InstallState = "installed" | "promptable" | "ios" | "unsupported";

let deferredPrompt: BeforeInstallPromptEvent | null = null;
let installedNow = false;
let captureStarted = false;
const listeners = new Set<() => void>();

const notify = () => listeners.forEach((l) => l());

export function startInstallPromptCapture() {
  if (captureStarted || typeof window === "undefined") return;
  captureStarted = true;
  window.addEventListener("beforeinstallprompt", (event) => {
    // Sans preventDefault, Chrome affiche sa propre mini-bannière tout de
    // suite : on préfère proposer l'installation à un moment choisi.
    event.preventDefault();
    deferredPrompt = event as BeforeInstallPromptEvent;
    notify();
  });
  window.addEventListener("appinstalled", () => {
    deferredPrompt = null;
    installedNow = true;
    notify();
  });
}

function isStandalone(): boolean {
  return (
    window.matchMedia("(display-mode: standalone)").matches ||
    (navigator as Navigator & { standalone?: boolean }).standalone === true
  );
}

export function isIosDevice(): boolean {
  const ua = navigator.userAgent;
  // iPadOS se présente comme un Mac : on le reconnaît à l'écran tactile.
  return /iPhone|iPad|iPod/.test(ua) || (/Macintosh/.test(ua) && navigator.maxTouchPoints > 1);
}

/** Navigateur tiers sur iOS (Chrome, Firefox...) : l'ajout à l'écran d'accueil passe par Safari. */
export function isIosNonSafari(): boolean {
  return /CriOS|FxiOS|EdgiOS|OPiOS/.test(navigator.userAgent);
}

export function getInstallState(): InstallState {
  if (installedNow || isStandalone()) return "installed";
  if (deferredPrompt) return "promptable";
  if (isIosDevice()) return "ios";
  return "unsupported";
}

export function getServerInstallState(): InstallState {
  return "unsupported";
}

export function subscribeInstallState(onChange: () => void) {
  listeners.add(onChange);
  return () => {
    listeners.delete(onChange);
  };
}

export async function promptInstall(): Promise<boolean> {
  if (!deferredPrompt) return false;
  const promptEvent = deferredPrompt;
  // Un événement ne peut servir qu'une fois.
  deferredPrompt = null;
  await promptEvent.prompt();
  const { outcome } = await promptEvent.userChoice;
  notify();
  return outcome === "accepted";
}
