export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`/api${path}`, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  });

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