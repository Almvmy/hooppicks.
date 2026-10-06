import { cn } from "@/lib/utils";

// Teinte stable dérivée du nom : chaque ligue garde sa couleur partout, sans
// rien stocker en base.
function hueFromName(name: string): number {
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = (hash * 31 + name.charCodeAt(i)) >>> 0;
  return hash % 360;
}

function initials(name: string): string {
  const words = name.trim().split(/\s+/).filter(Boolean);
  if (words.length === 0) return "?";
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return (words[0][0] + words[1][0]).toUpperCase();
}

/**
 * Blason d'une ligue : monogramme sur un écusson à sa couleur. Saturation et
 * clarté fixes (clarté 30 % : texte blanc ≥ 4,8:1 même sur la teinte la plus claire, le jaune-vert).
 */
export function LeagueCrest({ name, size = 40, className }: { name: string; size?: number; className?: string }) {
  const hue = hueFromName(name);
  return (
    <span
      aria-hidden
      className={cn("flex shrink-0 items-center justify-center rounded-[30%] font-heading font-bold text-white", className)}
      style={{
        width: size,
        height: size,
        fontSize: size * 0.38,
        background: `linear-gradient(145deg, hsl(${hue} 55% 30%), hsl(${(hue + 40) % 360} 60% 22%))`,
        boxShadow: `inset 0 1px 0 rgba(255,255,255,0.25), 0 6px 16px hsl(${hue} 55% 30% / 0.35)`,
      }}
    >
      {initials(name)}
    </span>
  );
}
