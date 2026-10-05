"use client";

import { useEffect, useRef, useState } from "react";
import { easeOutCubic, prefersReducedMotion } from "@/lib/motion";

/**
 * Nombre qui défile jusqu'à sa valeur. Anime chaque changement depuis la
 * valeur précédemment affichée (solde qui monte après un pari gagné), pas
 * depuis 0, sauf au premier affichage si animateOnMount.
 */
export function CountUp({
  value,
  format = (n) => n.toLocaleString("fr-FR"),
  duration = 900,
  animateOnMount = true,
}: {
  value: number;
  format?: (n: number) => string;
  duration?: number;
  animateOnMount?: boolean;
}) {
  const [displayed, setDisplayed] = useState(animateOnMount ? 0 : value);
  // Point de départ de la prochaine animation : la valeur réellement à
  // l'écran, pour qu'un changement en plein défilement reparte d'où il est.
  const shownRef = useRef(displayed);

  useEffect(() => {
    const from = shownRef.current;
    if (from === value) return;

    // Mouvement réduit : durée nulle, la valeur finale s'affiche dès la
    // première image (sans setState synchrone dans l'effet).
    const effectiveDuration = prefersReducedMotion() ? 0 : duration;
    const isInteger = Number.isInteger(value) && Number.isInteger(from);
    const start = performance.now();
    let frame = 0;

    const tick = (now: number) => {
      const progress = effectiveDuration === 0 ? 1 : Math.min((now - start) / effectiveDuration, 1);
      const raw = from + (value - from) * easeOutCubic(progress);
      const next = isInteger ? Math.round(raw) : raw;
      shownRef.current = next;
      setDisplayed(next);
      if (progress < 1) frame = requestAnimationFrame(tick);
    };

    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [value, duration]);

  // tabular-nums : chiffres à chasse fixe, sinon le texte "tremble" en
  // largeur à chaque image du défilement.
  return <span className="tabular-nums">{format(displayed)}</span>;
}
