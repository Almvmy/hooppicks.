import { NextRequest, NextResponse } from "next/server";

const PROTECTED_PREFIXES = [
  "/dashboard",
  "/matches",
  "/bets",
  "/leaderboard",
  "/profile",
  "/leagues",
  "/duels",
  "/saison",
];

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const isProtected = PROTECTED_PREFIXES.some((prefix) =>
    pathname.startsWith(prefix)
  );

  if (!isProtected) {
    return NextResponse.next();
  }

  const session = request.cookies.get("hp_session");

  if (!session) {
    const loginUrl = new URL("/login", request.url);
    // Chemin ET paramètres : un lien d'invitation (/leagues?code=…) doit
    // retrouver son code après la connexion.
    loginUrl.searchParams.set("from", pathname + request.nextUrl.search);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/dashboard/:path*", "/matches/:path*", "/bets/:path*", "/leaderboard/:path*", "/profile/:path*", "/leagues/:path*", "/duels/:path*", "/saison/:path*"],
};