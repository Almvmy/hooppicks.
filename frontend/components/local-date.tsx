"use client";

import { useTimeZone } from "@/lib/use-time-zone";
import { formatMatchDate, formatMatchTime } from "@/lib/utils";

// Pour les Server Components (page d'accueil) : la date est mise en forme
// côté navigateur, dans le fuseau de l'appareil ou celui choisi en
// paramètres, au lieu du fuseau du serveur.

export function MatchDate({ iso }: { iso: string }) {
  const timeZone = useTimeZone();
  return <>{formatMatchDate(new Date(iso), timeZone)}</>;
}

export function MatchTime({ iso }: { iso: string }) {
  const timeZone = useTimeZone();
  return <>{formatMatchTime(new Date(iso), timeZone)}</>;
}

/** "Europe/Paris" → "Paris" : nom de ville lisible pour une mention discrète. */
export function TimeZoneName() {
  const timeZone = useTimeZone();
  return <>{timeZone.split("/").pop()?.replace(/_/g, " ")}</>;
}
