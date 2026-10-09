import { useCallback, useSyncExternalStore } from "react";

// Avertit les autres composants de la page (l'événement « storage » ne part
// que vers les autres onglets).
const listeners = new Set<() => void>();
// Repli quand le stockage est refusé : le choix tient jusqu'au rechargement.
const memory = new Map<string, boolean>();

function read(key: string): boolean {
  try {
    const stored = window.localStorage.getItem(key);
    if (stored !== null) return stored === "1";
  } catch {
    // Stockage indisponible : on retombe sur la mémoire.
  }
  return memory.get(key) ?? false;
}

/**
 * Préférence d'affichage oui/non gardée dans le navigateur (ex. vue compacte
 * des matchs). Commodité seulement : stockage indisponible (navigation
 * privée…) = gardée en mémoire jusqu'au rechargement. Faux au rendu
 * serveur, sans décalage d'hydratation.
 */
export function useLocalFlag(key: string): [boolean, (value: boolean) => void] {
  const subscribe = useCallback((onChange: () => void) => {
    listeners.add(onChange);
    window.addEventListener("storage", onChange);
    return () => {
      listeners.delete(onChange);
      window.removeEventListener("storage", onChange);
    };
  }, []);
  const value = useSyncExternalStore(subscribe, () => read(key), () => false);
  const set = useCallback(
    (next: boolean) => {
      memory.set(key, next);
      try {
        window.localStorage.setItem(key, next ? "1" : "0");
      } catch {
        // Stockage indisponible : la préférence ne survivra pas au rechargement.
      }
      listeners.forEach((l) => l());
    },
    [key]
  );
  return [value, set];
}
