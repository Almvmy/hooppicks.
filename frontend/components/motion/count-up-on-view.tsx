"use client";

import { useEffect, useRef, useState } from "react";
import { CountUp } from "@/components/motion/count-up";

type Phase = "static" | "armed" | "running";

/**
 * CountUp qui ne démarre qu'une fois le nombre visible à l'écran : sur une
 * page longue, un compteur lancé au chargement aurait fini bien avant que
 * le visiteur ne descende jusqu'à lui.
 *
 * La vraie valeur est rendue par défaut (HTML serveur, robots, JS absent ou
 * observateur jamais déclenché) : on ne la remplace par 0 que si le bloc est
 * confirmé hors écran, donc invisible pendant ce passage à 0.
 */
export function CountUpOnView({
  value,
  format = (n: number) => n.toLocaleString("fr-FR"),
}: {
  value: number;
  format?: (n: number) => string;
}) {
  const ref = useRef<HTMLSpanElement>(null);
  const [phase, setPhase] = useState<Phase>("static");

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    let firstReport = true;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (firstReport) {
          firstReport = false;
          // Déjà à l'écran au chargement : la valeur est lue telle quelle.
          if (entry.isIntersecting) {
            observer.disconnect();
            return;
          }
          setPhase("armed");
          return;
        }
        if (entry.isIntersecting) {
          setPhase("running");
          observer.disconnect();
        }
      },
      { threshold: 0.6 }
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  return (
    <span ref={ref}>
      {phase === "static" ? (
        <span className="tabular-nums">{format(value)}</span>
      ) : (
        <CountUp
          value={phase === "running" ? value : 0}
          format={format}
          animateOnMount={false}
          duration={1400}
        />
      )}
    </span>
  );
}
