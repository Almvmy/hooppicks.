import { apiFetch } from "@/lib/api/http";
import { BetSelection, PlacedBet } from "@/lib/types";

export async function fetchBets(): Promise<PlacedBet[]> {
  return apiFetch<PlacedBet[]>("/bets");
}

export interface CashOutQuote {
  betId: string;
  value: number;
  available: boolean;
  /** Pourquoi c'est suspendu (fin de match serrée, direct indisponible…), null sinon. */
  suspended: string | null;
}

export const fetchCashOutQuotes = () => apiFetch<CashOutQuote[]>("/bets/cash-out");

/** Refus du serveur ; `value` : la nouvelle valeur quand elle a baissé depuis l'affichage. */
export class CashOutError extends Error {
  constructor(message: string, readonly value?: number) {
    super(message);
  }
}

export async function cashOutBet(betId: string, expected: number): Promise<PlacedBet> {
  try {
    return await apiFetch<PlacedBet>(`/bets/${betId}/cash-out`, { method: "POST", body: JSON.stringify({ expected }) });
  } catch (e) {
    const raw = e instanceof Error ? e.message : "";
    try {
      const body = JSON.parse(raw) as { message?: string; value?: number };
      throw new CashOutError(body.message ?? "Encaissement impossible.", body.value);
    } catch (parsed) {
      if (parsed instanceof CashOutError) throw parsed;
      throw e;
    }
  }
}

export async function placeBet(selections: BetSelection[], stake: number): Promise<PlacedBet> {
  return apiFetch<PlacedBet>("/bets", {
    method: "POST",
    body: JSON.stringify({ selections, stake }),
  });
}