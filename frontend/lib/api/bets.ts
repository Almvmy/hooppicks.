import { apiFetch } from "@/lib/api/http";
import { BetSelection, PlacedBet } from "@/lib/types";

export async function fetchBets(): Promise<PlacedBet[]> {
  return apiFetch<PlacedBet[]>("/bets");
}

export async function placeBet(selections: BetSelection[], stake: number): Promise<PlacedBet> {
  return apiFetch<PlacedBet>("/bets", {
    method: "POST",
    body: JSON.stringify({ selections, stake }),
  });
}