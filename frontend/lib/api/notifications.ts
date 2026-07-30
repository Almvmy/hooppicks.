import { apiFetch } from "@/lib/api/http";
import { AppNotification } from "@/lib/types";

export async function fetchNotifications(): Promise<AppNotification[]> {
  return apiFetch<AppNotification[]>("/notifications");
}

export async function markNotificationRead(id: string): Promise<void> {
  return apiFetch<void>(`/notifications/${id}/read`, { method: "PATCH" });
}