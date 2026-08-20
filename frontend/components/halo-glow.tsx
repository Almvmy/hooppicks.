import { cn } from "@/lib/utils";

/**
 * Halo lumineux orange — le seul endroit où la marque s'autorise du
 * brillant (cf. doc de marque, section système visuel). Purement
 * décoratif, à positionner en absolute derrière un élément clé.
 */
export function HaloGlow({ className }: { className?: string }) {
  return (
    <div
      aria-hidden
      className={cn(
        "pointer-events-none absolute rounded-full border border-primary/55",
        "shadow-[0_0_24px_rgba(255,122,26,.35),inset_0_0_18px_rgba(255,122,26,.18)]",
        className
      )}
    />
  );
}
