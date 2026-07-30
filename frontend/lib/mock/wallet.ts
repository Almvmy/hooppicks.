import { WalletData, WalletTransaction } from "@/lib/types";

const MOCK_BALANCE: WalletData = { balance: 1240 };

const MOCK_TRANSACTIONS: WalletTransaction[] = [
  { id: "1", type: "bet_win", amount: 150, description: "Lakers vs Celtics — Vainqueur", date: "2026-07-05T20:00:00Z" },
  { id: "2", type: "bet_loss", amount: -100, description: "Warriors vs Nets — Total de points", date: "2026-07-04T19:30:00Z" },
  { id: "3", type: "bet_placed", amount: -50, description: "Bucks vs Heat — Handicap", date: "2026-07-03T18:00:00Z" },
  { id: "4", type: "bonus", amount: 1000, description: "Capital de départ", date: "2026-07-01T10:00:00Z" },
];

// TEMPORAIRE : sera remplacé par fetch("/api/wallet") vers le backend NestJS.
export async function fetchWallet(): Promise<WalletData> {
  await new Promise((r) => setTimeout(r, 600));
  return MOCK_BALANCE;
}

export async function fetchWalletTransactions(): Promise<WalletTransaction[]> {
  await new Promise((r) => setTimeout(r, 800));
  return MOCK_TRANSACTIONS;
}