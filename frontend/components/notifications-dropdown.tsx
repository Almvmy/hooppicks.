"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Badge } from "@/components/ui/badge";
import { fetchNotifications, markNotificationRead } from "@/lib/api/notifications";
import { cn } from "@/lib/utils";

export function NotificationsDropdown() {
  const queryClient = useQueryClient();
  const { data } = useQuery({
    queryKey: ["notifications"],
    queryFn: fetchNotifications,
  });

  const unreadCount = data?.filter((n) => !n.read).length ?? 0;

  async function handleRead(id: string) {
    await markNotificationRead(id);
    queryClient.invalidateQueries({ queryKey: ["notifications"] });
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="relative rounded-full p-2 text-muted-foreground transition-colors hover:bg-accent hover:text-foreground">
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute right-1 top-1 flex h-4 w-4 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-primary-foreground">
            {unreadCount}
          </span>
        )}
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-80">
        {(!data || data.length === 0) && (
          <div className="px-3 py-4 text-center text-sm text-muted-foreground">
            Aucune notification.
          </div>
        )}
        {data?.map((notif) => (
          <DropdownMenuItem
            key={notif.id}
            onClick={() => !notif.read && handleRead(notif.id)}
            className={cn(
              "flex cursor-pointer items-start gap-2 whitespace-normal py-2",
              !notif.read && "bg-primary/5"
            )}
          >
            {!notif.read && (
              <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />
            )}
            <div className={cn(notif.read && "pl-3.5")}>
              <p className="text-sm">{notif.message}</p>
              <p className="mt-0.5 text-xs text-muted-foreground">
                {new Date(notif.date).toLocaleDateString("fr-FR", {
                  day: "2-digit",
                  month: "short",
                  hour: "2-digit",
                  minute: "2-digit",
                })}
              </p>
            </div>
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}