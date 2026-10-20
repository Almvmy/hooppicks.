export const PROTECTED_PREFIXES = [
  "/dashboard",
  "/matches",
  "/paris-joueurs",
  "/bets",
  "/leaderboard",
  "/profile",
  "/leagues",
  "/duels",
  "/contact",
  "/maillot",
];

export function isProtectedPath(pathname: string): boolean {
  return PROTECTED_PREFIXES.some((prefix) => pathname.startsWith(prefix));
}
