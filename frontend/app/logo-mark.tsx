import { LOGO_FULL, LOGO_MEDIUM, LOGO_SMALL } from "@/app/logo-geometry";

type LogoMarkProps = {
  /** "full" : avec le tir en pointillés (à partir de ~72 px).
   *  "medium" : pointillés plus gros, filet épaissi (36 à 72 px, en-têtes).
   *  "small" : filet épais, sans pointillés, lisible jusqu'à 16 px. */
  variant?: "full" | "medium" | "small";
  /** Couleurs : le ballon et ses coutures sont fixes (marque), la jante et
   *  le filet suivent le texte (blanc sur Nuit, marine sur sable). */
  ball?: string;
  seam?: string;
  rim?: string;
  arc?: string;
  /** Classe appliquée aux points du tir (thème clair : sarcelle). */
  arcClassName?: string;
  className?: string;
  width?: number;
  height?: number;
};

/**
 * Symbole HoopPicks : un tir qui entre dans le panier.
 * Dessin partagé par le logo de l'interface (LogoSymbol) et les icônes
 * d'app (brand-icon) pour qu'ils ne divergent jamais. Aucun hook, aucun
 * clipPath : le même rendu passe dans le DOM et dans next/og (satori).
 */
export function LogoMark({
  variant = "small",
  ball = "#FF7A1A",
  seam = "#0B1120",
  rim = "currentColor",
  arc = "#00E5FF",
  arcClassName,
  className,
  width,
  height,
}: LogoMarkProps) {
  const g = variant === "full" ? LOGO_FULL : variant === "medium" ? LOGO_MEDIUM : LOGO_SMALL;
  return (
    <svg
      viewBox={g.viewBox}
      width={width}
      height={height}
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-hidden="true"
    >
      {g.dots.map(([cx, cy, r], i) => (
        <circle key={i} cx={cx} cy={cy} r={r} fill={arc} className={arcClassName} />
      ))}
      <path d={g.backRim} stroke={rim} strokeWidth={g.backRimW} strokeLinecap="round" opacity={0.5} />
      <circle cx={g.ball.cx} cy={g.ball.cy} r={g.ball.r} fill={ball} />
      {g.seams.map((d, i) => (
        <path key={i} d={d} stroke={seam} strokeWidth={g.seamW} strokeLinecap={i < 2 ? "butt" : "round"} />
      ))}
      <path d={g.net} stroke={rim} strokeWidth={g.netW} strokeLinecap="round" strokeLinejoin="round" />
      <path d={g.edge} stroke={rim} strokeWidth={g.netW} strokeLinecap="round" />
      <path d={g.frontRim} stroke={rim} strokeWidth={g.rimW} strokeLinecap="round" />
    </svg>
  );
}
