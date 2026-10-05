import { cn } from "@/lib/utils";

// Logo de la ligue tel que le renvoie l'API ESPN (leagues[0].logos), même
// CDN que les logos d'équipe. ESPN en fournit une version pour fond sombre :
// on affiche la bonne selon le thème, sans JS (pas d'écart d'hydratation).
const NBA_LOGO_LIGHT = "https://a.espncdn.com/i/teamlogos/leagues/500/nba.png";
const NBA_LOGO_DARK =
  "https://a.espncdn.com/combiner/i?img=/i/teamlogos/leagues/500-dark/nba.png&w=500&h=500&transparent=true";

export function NbaLogo({ size = 16, className }: { size?: number; className?: string }) {
  return (
    <span className={cn("inline-flex shrink-0", className)} style={{ width: size, height: size }}>
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={NBA_LOGO_LIGHT} alt="NBA" width={size} height={size} className="object-contain dark:hidden" />
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={NBA_LOGO_DARK} alt="NBA" width={size} height={size} className="hidden object-contain dark:block" />
    </span>
  );
}
