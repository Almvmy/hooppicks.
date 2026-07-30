import { PlacedBet } from "@/lib/types";

let bets: PlacedBet[] = [
  {
    id: "b1",
    selections: [
      { id: "m6-moneyline-away", matchId: "m6", matchLabel: "Heat vs Lakers", market: "moneyline", outcome: "away", label: "Lakers (vainqueur)", odds: 1.6 },
    ],
    stake: 100,
    totalOdds: 1.6,
    potentialPayout: 160,
    status: "won",
    placedAt: "2026-07-07T15:00:00Z",
  },
  {
    id: "b2",
    selections: [
      { id: "m4-total-over", matchId: "m4", matchLabel: "Nuggets vs Suns", market: "total", outcome: "over", label: "Plus de 216.5 pts", odds: 1.9 },
    ],
    stake: 50,
    totalOdds: 1.9,
    potentialPayout: 95,
    status: "lost",
    placedAt: "2026-07-08T18:00:00Z",
  },
];

// TEMPORAIRE : sera remplacé par les endpoints /api/bets (GET) et /api/bets (POST) du backend.
export async function fetchBets(): Promise<PlacedBet[]> {
  await new Promise((r) => setTimeout(r, 500));
  return bets;
}

export async function placeBet(bet: PlacedBet): Promise<PlacedBet> {
  await new Promise((r) => setTimeout(r, 500));
  bets = [bet, ...bets];
  return bet;
}