import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api/http";

/** Premier match de la saison régulière et soirée d'ouverture (SeasonLaunchController). */
export interface SeasonLaunch {
  season: string;
  openerMatchId: string;
  openerDate: string;
  openingNightStart: string;
  openingNightEnd: string;
}

export type LaunchPhase = "before" | "opening-night" | "after";

export function useSeasonLaunch(): SeasonLaunch | null | undefined {
  const { data } = useQuery({
    queryKey: ["season", "launch"],
    queryFn: async () => (await apiFetch<SeasonLaunch | undefined>("/season/launch")) ?? null,
    staleTime: 10 * 60 * 1000,
  });
  return data;
}

export function launchPhase(launch: SeasonLaunch, now: number): LaunchPhase {
  if (now < new Date(launch.openerDate).getTime()) return "before";
  if (now < new Date(launch.openingNightEnd).getTime()) return "opening-night";
  return "after";
}

/** Heure courante, rafraîchie chaque seconde (compte à rebours). */
export function useNow(intervalMs = 1000): number {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), intervalMs);
    return () => clearInterval(id);
  }, [intervalMs]);
  return now;
}

export function countdownParts(ms: number) {
  const total = Math.max(0, Math.floor(ms / 1000));
  return {
    days: Math.floor(total / 86400),
    hours: Math.floor((total % 86400) / 3600),
    minutes: Math.floor((total % 3600) / 60),
    seconds: total % 60,
  };
}
