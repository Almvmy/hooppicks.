/**
 * Filigrane discret d'un morceau de terrain (arc + raquette) en fond de
 * carte. Purement décoratif et vectoriel : aucun logo, aucune marque.
 * Réutilisé sur le hero du dashboard, le profil et le classement pour une
 * identité visuelle cohérente.
 */
export function CourtWatermark({ className }: { className?: string }) {
  return (
    <svg
      aria-hidden
      viewBox="0 0 400 260"
      className={
        className ??
        "pointer-events-none absolute -right-10 -top-8 h-[260px] w-[400px] opacity-[0.06] sm:opacity-[0.08]"
      }
    >
      <circle cx="330" cy="130" r="90" fill="none" stroke="var(--primary)" strokeWidth="2" />
      <path d="M400 40 A120 120 0 0 1 400 220" fill="none" stroke="var(--primary)" strokeWidth="2" />
      <rect x="330" y="70" width="70" height="120" fill="none" stroke="var(--primary)" strokeWidth="2" />
      <line x1="330" y1="10" x2="330" y2="250" stroke="var(--primary)" strokeWidth="2" />
    </svg>
  );
}
