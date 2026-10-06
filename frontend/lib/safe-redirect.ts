/**
 * Destination après connexion/inscription (?from=…), limitée aux chemins
 * internes : un "from" vers un autre site ("//evil.com", "https://…")
 * ferait de la page de connexion une redirection ouverte, exploitable en
 * hameçonnage. Tout le reste retombe sur le tableau de bord.
 */
export function safeRedirectTarget(from: string | undefined | null): string {
  if (!from || !from.startsWith("/") || from.startsWith("//") || from.startsWith("/\\")) return "/dashboard";
  return from;
}
