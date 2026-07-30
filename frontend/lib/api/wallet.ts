import { apiFetch } from "@/lib/api/http";
import { WalletData, WalletTransaction } from "@/lib/types";

export async function fetchWallet(): Promise<WalletData> {
  return apiFetch<WalletData>("/wallet");
}

export async function fetchWalletTransactions(): Promise<WalletTransaction[]> {
  return apiFetch<WalletTransaction[]>("/wallet/transactions");
}