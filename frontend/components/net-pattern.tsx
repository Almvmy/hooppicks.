/**
 * Filigrane de filet — lignes en éventail traversées de deux courbes.
 * Purement décoratif et vectoriel, cf. CourtWatermark.
 */
export function NetPattern({ className }: { className?: string }) {
  return (
    <svg
      aria-hidden
      viewBox="0 0 200 120"
      preserveAspectRatio="none"
      className={
        className ??
        "pointer-events-none absolute inset-0 h-full w-full opacity-[0.07] sm:opacity-[0.09]"
      }
    >
      <path
        d="M20 0 L60 120 M60 0 L84 120 M100 0 L104 120 M140 0 L126 120 M180 0 L148 120"
        stroke="var(--muted-foreground)"
        strokeWidth="1.5"
      />
      <path
        d="M10 34 Q100 62 190 34 M28 70 Q100 94 172 70"
        stroke="var(--muted-foreground)"
        strokeWidth="1.5"
        fill="none"
      />
    </svg>
  );
}
