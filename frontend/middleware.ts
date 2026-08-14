import { NextRequest, NextResponse } from "next/server";

// NOTE : le backend (Railway) et le frontend (Vercel) sont sur des domaines
// différents. Le cookie de session "hp_session" est donc un cookie cross-site :
// le navigateur ne l'envoie qu'au domaine du backend, jamais aux requêtes
// faites vers ce serveur Next.js. Ce middleware ne peut donc JAMAIS voir ce
// cookie et ne doit pas s'en servir pour rediriger, sous peine de boucle de
// redirection permanente vers /login.
//
// La protection des routes est désormais assurée côté client par
// <RequireAuth> (components/auth/require-auth.tsx), qui interroge
// GET /auth/me sur le backend avec `credentials: "include"` — seul endroit
// où le cookie cross-site est effectivement envoyé.

export function middleware(_request: NextRequest) {
  return NextResponse.next();
}

export const config = {
  matcher: [],
};