import { apiFetch } from "@/lib/api/http";
import { AppNotification } from "@/lib/types";

export async function fetchNotifications(): Promise<AppNotification[]> {
  return apiFetch<AppNotification[]>("/notifications");
}

export async function markNotificationRead(id: string): Promise<void> {
  return apiFetch<void>(`/notifications/${id}/read`, { method: "PATCH" });
}
export async function fetchUnreadCount(): Promise<number> {
  const r = await apiFetch<{ count: number }>("/notifications/unread-count");
  return r.count;
}

export async function markAllNotificationsRead(): Promise<void> {
  return apiFetch<void>("/notifications/read-all", { method: "PATCH" });
}
