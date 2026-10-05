// Lu au moment de lancer une animation plutôt qu'au rendu : évite un état
// React de plus et un écart d'hydratation (le serveur ne connaît pas la
// préférence de l'appareil).
export function prefersReducedMotion(): boolean {
  return (
    typeof window !== "undefined" &&
    window.matchMedia("(prefers-reduced-motion: reduce)").matches
  );
}

export function easeOutCubic(t: number): number {
  return 1 - Math.pow(1 - t, 3);
}
