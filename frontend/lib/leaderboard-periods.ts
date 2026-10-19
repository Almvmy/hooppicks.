import { useQuery } from "@tanstack/react-query";
import { fetchLeaderboardPhases } from "@/lib/api/leaderboard";
import { LeaderboardPeriod, LeaderboardPhase } from "@/lib/types";

export type PeriodOption = { value: LeaderboardPeriod; label: string; scope: string };

const ALL_PERIODS: PeriodOption[] = [
  { value: "season", label: "Saison", scope: "de la saison" },
  { value: "month", label: "Ce mois-ci", scope: "du mois" },
  { value: "week", label: "Cette semaine", scope: "de la semaine de jeu" },
  { value: "cup", label: "Coupe NBA", scope: "de la Coupe NBA" },
  { value: "playoffs", label: "Playoffs", scope: "des playoffs" },
];

export function isPhase(period: LeaderboardPeriod): period is LeaderboardPhase {
  return period === "cup" || period === "playoffs";
}

export function periodOption(period: LeaderboardPeriod): PeriodOption {
  return ALL_PERIODS.find((p) => p.value === period)!;
}

/**
 * Périodes du classement : Coupe NBA et playoffs n'apparaissent qu'une fois
 * leur premier match commencé (GET /leaderboard/phases).
 */
export function useLeaderboardPeriods(): PeriodOption[] {
  const { data } = useQuery({ queryKey: ["leaderboard-phases"], queryFn: fetchLeaderboardPhases, staleTime: 30 * 60 * 1000 });
  return ALL_PERIODS.filter((p) => !isPhase(p.value) || data?.[p.value]);
}
