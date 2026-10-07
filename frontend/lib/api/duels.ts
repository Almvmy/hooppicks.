import { apiFetch } from "@/lib/api/http";
import { Duel } from "@/lib/types";

export async function fetchDuels(): Promise<Duel[]> {
  return apiFetch<Duel[]>("/duels");
}

export async function challengeToDuel(opponent: string): Promise<{ id: string }> {
  return apiFetch<{ id: string }>("/duels", { method: "POST", body: JSON.stringify({ opponent }) });
}

export async function respondToDuel(id: string, action: "accept" | "decline" | "cancel"): Promise<{ status: string }> {
  return apiFetch<{ status: string }>(`/duels/${id}/${action}`, { method: "POST" });
}
