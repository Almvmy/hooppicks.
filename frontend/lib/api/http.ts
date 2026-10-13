import { isProtectedPath } from "@/lib/protected-routes";

// Un mauvais mot de passe répond aussi 401 : ce n'est pas une session expirée.
const AUTH_FORMS = ["/auth/login", "/auth/register"];

let redirecting = false;

// Sessions en mémoire côté backend : après un redémarrage, le cookie reste
// mais n'est plus reconnu. Sans ça, chaque action affichait « Erreur API
// (401) » à un joueur qui se croyait connecté.
async function handleExpiredSession(isAction: boolean): Promise<boolean> {
  if (typeof window === "undefined") return false;
  const { pathname, search } = window.location;
  // Un visiteur anonyme sur une page publique (actus, joueurs) n'est pas
  // renvoyé à la connexion pour un simple affichage.
  if (!isAction && !isProtectedPath(pathname)) return false;
  if (!redirecting) {
    redirecting = true;
    await fetch("/api/auth/logout", { method: "POST", credentials: "include" }).catch(() => undefined);
    // Rechargement complet voulu : on repart sans le cache de l'ancienne session.
    // eslint-disable-next-line @next/next/no-location-assign-relative-destination
    window.location.assign(`/login?expired=1&from=${encodeURIComponent(pathname + search)}`);
  }
  return true;
}

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`/api${path}`, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  });

  if (res.status === 401 && !AUTH_FORMS.includes(path)) {
    const isAction = !!options.method && options.method.toUpperCase() !== "GET";
    if (await handleExpiredSession(isAction)) {
      throw new Error("Ta session a expiré, reconnecte-toi.");
    }
  }

  if (!res.ok) {
    // Les endpoints d'auth renvoient parfois un message texte brut ("Mot de
    // passe incorrect.") plutôt qu'un JSON : on le remonte tel quel s'il
    // existe, pour l'afficher directement à l'utilisateur.
    const body = await res.text().catch(() => "");
    throw new Error(body || `Erreur API (${res.status}) sur ${path}`);
  }

  const text = await res.text();
  return text ? (JSON.parse(text) as T) : (undefined as T);
}
