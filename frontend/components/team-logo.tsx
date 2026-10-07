"use client";

import { useEffect, useRef, useState, ViewTransition } from "react";
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
  // Image injoignable (CDN ESPN bloqué, réseau coupé) : repli sur le disque
  // de couleur plutôt qu'une image cassée avec son texte de remplacement.
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const imgRef = useRef<HTMLImageElement>(null);
  // Une image rendue côté serveur peut échouer avant que React soit branché :
  // onError ne part alors jamais, on vérifie son état au montage.
  useEffect(() => {
    const img = imgRef.current;
    if (img && logoUrl && img.complete && img.naturalWidth === 0) setFailedUrl(logoUrl);
  }, [logoUrl]);

  if (logoUrl && failedUrl !== logoUrl) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={logoUrl}
        alt={abbreviation}
        ref={imgRef}
        onError={() => setFailedUrl(logoUrl)}
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
