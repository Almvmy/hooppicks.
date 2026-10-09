import { cn } from "@/lib/utils";

// Logo de la ligue (ESPN, version claire et version fond sombre), servi par
// l'app elle-même : la version sombre passait par le redimensionneur d'ESPN
// (combiner), que Safari iOS n'affichait pas (image cassée). La bonne version
// selon le thème, sans JS (pas d'écart d'hydratation).
const NBA_LOGO_LIGHT = "/images/nba-logo.png";
const NBA_LOGO_DARK = "/images/nba-logo-dark.png";

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
