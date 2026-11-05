"use client";

import { useSyncExternalStore } from "react";
import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api/http";
import type { NavTab } from "@/lib/nav";

const fetchSeasonalTabs = () => apiFetch<{ cup: boolean; season: boolean }>("/nav/seasonal");
const noSubscribe = () => () => {};

/**
 * Les onglets d'une rubrique à afficher : ceux de saison seulement quand ils
 * servent, sauf si on est dessus (arrivé par un lien, l'onglet actif ne doit pas
 * disparaître). Si le serveur ne répond pas, on les montre tous plutôt que d'en cacher.
 *
 * `hydrated` : les onglets sont sous Suspense, donc raccordés après coup. La réponse
 * pouvait déjà être là, et la page ne correspondait plus au rendu du serveur.
 */
export function useSectionTabs(tabs: NavTab[] | undefined, pathname: string): NavTab[] {
  const hydrated = useSyncExternalStore(noSubscribe, () => true, () => false);
  const { data, isError } = useQuery({
    queryKey: ["nav", "seasonal"],
    queryFn: fetchSeasonalTabs,
    staleTime: 10 * 60 * 1000,
    enabled: !!tabs?.some((t) => t.seasonal),
  });
  return (tabs ?? []).filter((t) => !t.seasonal || t.href === pathname || (hydrated && (isError || data?.[t.seasonal])));
}
