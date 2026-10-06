// Fuseau horaire d'affichage des dates/heures. Préférence par appareil
// (localStorage) : quelqu'un qui voyage ou suit les matchs à l'heure US
// règle son téléphone sans toucher à son compte.

// Rendu serveur et valeur par défaut : les joueurs sont en Côte d'Ivoire.
// Sans fuseau explicite, le rendu serveur dépendrait de celui de la machine.
export const DEFAULT_TIME_ZONE = "Africa/Abidjan";
export const AUTO_TIME_ZONE = "auto";

const STORAGE_KEY = "hp:time-zone";

export const TIME_ZONE_CHOICES: { value: string; label: string }[] = [
  { value: "Africa/Abidjan", label: "Côte d'Ivoire (GMT)" },
  { value: "Africa/Dakar", label: "Sénégal, Mali, Burkina Faso (GMT)" },
  { value: "Africa/Lagos", label: "Bénin, Niger, Nigeria" },
  { value: "Europe/Paris", label: "France métropolitaine, Belgique, Suisse" },
  { value: "Europe/London", label: "Royaume-Uni, Portugal" },
  { value: "Africa/Casablanca", label: "Maroc" },
  { value: "Africa/Algiers", label: "Algérie, Tunisie" },
  { value: "Africa/Douala", label: "Cameroun, Congo" },
  { value: "Indian/Reunion", label: "La Réunion" },
  { value: "America/Martinique", label: "Martinique, Guadeloupe" },
  { value: "America/Montreal", label: "Québec" },
  { value: "America/New_York", label: "Heure de l'Est US (heure des matchs)" },
  { value: "America/Los_Angeles", label: "Heure du Pacifique US" },
];

const listeners = new Set<() => void>();
let cachedPreference: string | null = null;

export function getTimeZonePreference(): string {
  if (typeof window === "undefined") return AUTO_TIME_ZONE;
  if (cachedPreference === null) {
    try {
      cachedPreference = window.localStorage.getItem(STORAGE_KEY) ?? AUTO_TIME_ZONE;
    } catch {
      cachedPreference = AUTO_TIME_ZONE;
    }
  }
  return cachedPreference;
}

export function setTimeZonePreference(value: string) {
  cachedPreference = value;
  try {
    if (value === AUTO_TIME_ZONE) window.localStorage.removeItem(STORAGE_KEY);
    else window.localStorage.setItem(STORAGE_KEY, value);
  } catch {
    // Stockage indisponible (navigation privée...) : le choix vaut pour la session.
  }
  listeners.forEach((notify) => notify());
}

export function detectDeviceTimeZone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone || DEFAULT_TIME_ZONE;
}

/** Fuseau effectivement utilisé pour formater : préférence, sinon celui de l'appareil. */
export function resolveTimeZone(): string {
  if (typeof window === "undefined") return DEFAULT_TIME_ZONE;
  const preference = getTimeZonePreference();
  return preference === AUTO_TIME_ZONE ? detectDeviceTimeZone() : preference;
}

export function subscribeTimeZone(onChange: () => void) {
  listeners.add(onChange);
  // Changement fait dans un autre onglet.
  const onStorage = (e: StorageEvent) => {
    if (e.key !== STORAGE_KEY) return;
    cachedPreference = null;
    onChange();
  };
  window.addEventListener("storage", onStorage);
  return () => {
    listeners.delete(onChange);
    window.removeEventListener("storage", onStorage);
  };
}
