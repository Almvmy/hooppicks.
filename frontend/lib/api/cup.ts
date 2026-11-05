import { apiFetch } from "@/lib/api/http";

export interface CupTeamRow {
  teamId: string | null;
  abbreviation: string;
  name: string;
  logoUrl: string | null;
  wins: number;
  losses: number;
  diff: number;
  pointsFor: number;
  /** Connu seulement quand tous les matchs de la conférence sont joués. */
  status: "qualified" | "wildcard" | "eliminated" | null;
}

export interface CupGroup {
  conference: "Est" | "Ouest";
  name: string;
  teams: CupTeamRow[];
}

export const fetchCupStandings = () => apiFetch<{ groups: CupGroup[] }>("/cup/standings");
