import { useQuery } from "@tanstack/react-query";
import { fetchLeaderboardPhases } from "@/lib/api/leaderboard";
import { ChampionPhase, LeaderboardPeriod, LeaderboardPhase } from "@/lib/types";

export type PeriodOption = { value: LeaderboardPeriod; label: string; scope: string };

const ALL_PERIODS: PeriodOption[] = [
  { value: "season", label: "Saison", scope: "de la saison" },
  { value: "month", label: "Ce mois-ci", scope: "du mois" },
  { value: "week", label: "Cette semaine", scope: "de la semaine de jeu" },
  { value: "cup", label: "Coupe NBA", scope: "de la Coupe NBA" },
  { value: "playoffs", label: "Playoffs", scope: "des playoffs" },
  { value: "general", label: "Général", scope: "de toute la saison" },
];

export function isPhase(period: LeaderboardPeriod): period is LeaderboardPhase {
  return period === "cup" || period === "playoffs";
}

/**
 * Titre lié à la période : celui du mois passé sur « Ce mois-ci », de la
 * phase sinon ; null (semaine, saison) : le champion de la semaine passée.
 */
export function championPhase(period: LeaderboardPeriod): ChampionPhase | null {
  return period === "season" || period === "week" ? null : period;
}

export function periodOption(period: LeaderboardPeriod): PeriodOption {
  return ALL_PERIODS.find((p) => p.value === period)!;
}

/**
 * Périodes du classement : Coupe NBA et playoffs n'apparaissent qu'une fois
 * leur premier match commencé (GET /leaderboard/phases). Le général (toute
 * la saison, playoffs compris) arrive avec les playoffs : avant, il ne
 * diffère pas du classement de la saison.
 */
export function useLeaderboardPeriods(): PeriodOption[] {
  const { data } = useQuery({ queryKey: ["leaderboard-phases"], queryFn: fetchLeaderboardPhases, staleTime: 30 * 60 * 1000 });
  return ALL_PERIODS.filter((p) => (p.value === "general" ? data?.playoffs : !isPhase(p.value) || data?.[p.value]));
}
