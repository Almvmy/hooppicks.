/**
 * Filigrane de coutures de ballon : courbes répétées en fond de carte.
 * Purement décoratif et vectoriel, cf. CourtWatermark.
 */
export function SeamPattern({ className }: { className?: string }) {
  return (
    <svg
      aria-hidden
      viewBox="0 0 200 120"
      preserveAspectRatio="none"
      className={
        className ??
        "pointer-events-none absolute inset-0 h-full w-full opacity-[0.06] sm:opacity-[0.08]"
      }
    >
      <path
        d="M-10 20 C40 46 40 74 -10 100 M60 10 C110 42 110 78 60 110 M130 10 C180 42 180 78 130 110"
        stroke="var(--primary)"
        strokeWidth="2"
        fill="none"
      />
    </svg>
  );
}
