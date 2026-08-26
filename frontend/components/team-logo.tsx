import { getTeamColor } from "@/lib/team-colors";
import { cn } from "@/lib/utils";

/**
 * Logo officiel (CDN ESPN) quand disponible, sinon le disque de couleur +
 * sigle qu'on utilisait déjà avant — jamais bloquant si logoUrl est encore
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
