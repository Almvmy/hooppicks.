import { apiFetch } from "@/lib/api/http";
import { UserBadge } from "@/lib/types";

export async function fetchBadges(): Promise<UserBadge[]> {
  return apiFetch<UserBadge[]>("/badges");
}