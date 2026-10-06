import { useEffect, useState } from "react";

export type OddsTrend = "up" | "down" | null;

// Dernière valeur vue par sélection (id de BetSelection), partagée au niveau
// du module : persiste tant que l'onglet reste ouvert, peu importe quelle
// page affiche la cote. Volontairement hors de React : ce n'est que de la
// mémoire de comparaison, pas un état à synchroniser/persister.
const lastSeenBySelectionId = new Map<string, number>();

const FLASH_DURATION_MS = 2500;

function compareOdds(previous: number | undefined, odds: number): OddsTrend {
  if (previous === undefined || previous === odds) return null;
  return odds > previous ? "up" : "down";
}

/**
 * Détecte qu'une cote vient de changer depuis la dernière fois qu'elle a été
 * rendue (n'importe où dans l'app), et renvoie un sens ("up"/"down") pendant
 * quelques secondes pour permettre un flash visuel : sans rien demander de
 * plus au backend, qui recalcule déjà les cotes en continu.
 */
export function useOddsTrend(selectionId: string, odds: number): OddsTrend {
  // Au montage, on compare à la mémoire du module : une cote qui a bougé
  // pendant qu'on était sur une autre page doit flasher dès l'arrivée.
  const [seen, setSeen] = useState({ selectionId, odds });
  const [flash, setFlash] = useState(() => ({
    trend: compareOdds(lastSeenBySelectionId.get(selectionId), odds),
    // Change à chaque nouveau flash : deux hausses d'affilée donnent le même
    // sens, il faut quand même relancer le minuteur.
    id: 0,
  }));

  // Ajusté pendant le rendu plutôt que dans un effet (même schéma que
  // `previousCount` dans bet-slip-panel) : le flash part dès ce rendu-ci, sans
  // second rendu en cascade. Si le composant passe à une autre sélection, sa
  // référence est la mémoire du module, pas la cote de l'ancienne sélection.
  if (seen.selectionId !== selectionId || seen.odds !== odds) {
    const previous =
      seen.selectionId === selectionId ? seen.odds : lastSeenBySelectionId.get(selectionId);
    setSeen({ selectionId, odds });
    setFlash((f) => ({ trend: compareOdds(previous, odds), id: f.id + 1 }));
  }

  // Écrire dans la Map est un effet de bord : hors du rendu, qui doit rester
  // pur (StrictMode le rejoue deux fois).
  useEffect(() => {
    lastSeenBySelectionId.set(selectionId, odds);
  }, [selectionId, odds]);

  useEffect(() => {
    if (!flash.trend) return;
    const timeout = setTimeout(
      () => setFlash((f) => ({ ...f, trend: null })),
      FLASH_DURATION_MS,
    );
    return () => clearTimeout(timeout);
  }, [flash.trend, flash.id]);

  return flash.trend;
}
