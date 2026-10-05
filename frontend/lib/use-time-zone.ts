"use client";

import { useSyncExternalStore } from "react";
import { DEFAULT_TIME_ZONE, resolveTimeZone, subscribeTimeZone } from "@/lib/time-zone";

/**
 * Fuseau d'affichage, réactif aux changements de préférence. Côté serveur et
 * à l'hydratation il vaut DEFAULT_TIME_ZONE (même rendu des deux côtés, pas
 * d'erreur d'hydratation), puis bascule sur le fuseau réel de l'appareil.
 */
export function useTimeZone(): string {
  return useSyncExternalStore(subscribeTimeZone, resolveTimeZone, () => DEFAULT_TIME_ZONE);
}
