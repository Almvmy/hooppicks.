import { useEffect, useRef, useState } from "react";

export type OddsTrend = "up" | "down" | null;

// Dernière valeur vue par sélection (id de BetSelection), partagée au niveau
// du module — persiste tant que l'onglet reste ouvert, peu importe quelle
// page affiche la cote. Volontairement hors de React : ce n'est que de la
// mémoire de comparaison, pas un état à synchroniser/persister.
const lastSeenBySelectionId = new Map<string, number>();

const FLASH_DURATION_MS = 2500;

/**
 * Détecte qu'une cote vient de changer depuis la dernière fois qu'elle a été
 * rendue (n'importe où dans l'app), et renvoie un sens ("up"/"down") pendant
 * quelques secondes pour permettre un flash visuel — sans rien demander de
 * plus au backend, qui recalcule déjà les cotes en continu.
 */
export function useOddsTrend(selectionId: string, odds: number): OddsTrend {
  const [trend, setTrend] = useState<OddsTrend>(null);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    const previous = lastSeenBySelectionId.get(selectionId);

    if (previous !== undefined && previous !== odds) {
      setTrend(odds > previous ? "up" : "down");
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      timeoutRef.current = setTimeout(() => setTrend(null), FLASH_DURATION_MS);
    }

    lastSeenBySelectionId.set(selectionId, odds);

    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectionId, odds]);

  return trend;
}
