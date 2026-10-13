export const PROTECTED_PREFIXES = [
  "/dashboard",
  "/matches",
  "/bets",
  "/leaderboard",
  "/profile",
  "/leagues",
  "/duels",
  "/saison",
];

export function isProtectedPath(pathname: string): boolean {
  return PROTECTED_PREFIXES.some((prefix) => pathname.startsWith(prefix));
}
