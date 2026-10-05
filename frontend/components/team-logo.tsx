import { ViewTransition } from "react";
import { getTeamColor } from "@/lib/team-colors";
import { cn } from "@/lib/utils";

/**
 * Variante qui "vole" d'une page à l'autre : deux MorphingTeamLogo de même
 * morphName, l'un sur la page quittée et l'autre sur la page d'arrivée, sont
 * animés par le navigateur comme un seul objet qui change de taille et de
 * place. morphName doit être unique sur une même page (React avertit sinon).
 */
export function MorphingTeamLogo({
  morphName,
  ...props
}: React.ComponentProps<typeof TeamLogo> & { morphName: string }) {
  return (
    <ViewTransition name={morphName} share="team-morph">
      <TeamLogo {...props} />
    </ViewTransition>
  );
}

export function teamLogoMorphName(matchId: string, side: "home" | "away") {
  return `team-logo-${matchId}-${side}`;
}

/**
 * Logo officiel (CDN ESPN) quand disponible, sinon le disque de couleur +
 * sigle qu'on utilisait déjà avant : jamais bloquant si logoUrl est encore
 * null (équipe pas encore synchronisée, cf. EspnStandingsService).
 */
export function TeamLogo({
  abbreviation,
  logoUrl,
  size = 28,
  className,
}: {
  abbreviation: string;
  logoUrl?: string | null;
  size?: number;
  className?: string;
}) {
  if (logoUrl) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={logoUrl}
        alt={abbreviation}
        width={size}
        height={size}
        className={cn("shrink-0 object-contain", className)}
      />
    );
  }

  return (
    <span
      aria-hidden
      className={cn("flex shrink-0 items-center justify-center rounded-full font-mono font-bold text-white", className)}
      style={{
        width: size,
        height: size,
        backgroundColor: getTeamColor(abbreviation),
        fontSize: size * 0.32,
      }}
    >
      {abbreviation}
    </span>
  );
}
