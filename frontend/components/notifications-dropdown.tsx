"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { fetchNotifications, fetchUnreadCount, markAllNotificationsRead } from "@/lib/api/notifications";
import { AppNotification, NotificationType } from "@/lib/types";
import { cn, formatShortDateTime } from "@/lib/utils";

// Où mène une notification : la cloche n'en montre qu'une phrase, le détail est sur la page.
const DESTINATION: Partial<Record<NotificationType, string>> = {
  bet_won: "/bets",
  bet_lost: "/bets",
  bet_progress: "/bets",
  duel: "/duels",
  match_starting: "/matches",
  match_follow: "/matches",
  favorite_team: "/matches",
  feedback_reply: "/contact",
};

export function NotificationsDropdown() {
  const queryClient = useQueryClient();
  const router = useRouter();
  // Non lues au moment d'ouvrir : elles restent signalées tant que la cloche est ouverte,
  // même si elles viennent d'être marquées lues côté serveur.
  const [freshIds, setFreshIds] = useState<Set<string>>(new Set());

  const { data } = useQuery({ queryKey: ["notifications"], queryFn: fetchNotifications });
  const { data: unreadCount = 0 } = useQuery({ queryKey: ["notifications", "unread-count"], queryFn: fetchUnreadCount });

  // Ouvrir la cloche vaut lecture : avec les matchs suivis, on en reçoit trop
  // pour les marquer lues une par une.
  async function handleOpenChange(open: boolean) {
    if (!open) {
      setFreshIds(new Set());
      return;
    }
    if (unreadCount === 0) return;
    // Liste rechargée avant de la marquer lue : celle en cache peut dater d'avant les dernières arrivées.
    const list = await queryClient.fetchQuery({ queryKey: ["notifications"], queryFn: fetchNotifications, staleTime: 0 });
    setFreshIds(new Set(list.filter((n) => !n.read).map((n) => n.id)));

    queryClient.setQueryData(["notifications", "unread-count"], 0);
    queryClient.setQueryData<AppNotification[]>(["notifications"], (old) => old?.map((n) => ({ ...n, read: true })));
    try {
      await markAllNotificationsRead();
    } finally {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    }
  }

  function handleClick(notif: AppNotification) {
    const destination = DESTINATION[notif.type];
    if (destination) router.push(destination);
  }

  return (
    <DropdownMenu onOpenChange={(open) => handleOpenChange(open)}>
      <DropdownMenuTrigger
        aria-label={unreadCount > 0 ? `Notifications, ${unreadCount} non lue(s)` : "Notifications"}
        className="relative rounded-full p-2 text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute right-0.5 top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-bold text-primary-foreground">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="max-h-[70vh] w-80 overflow-y-auto">
        {(!data || data.length === 0) && (
          <div className="px-3 py-4 text-center text-sm text-muted-foreground">
            Aucune notification.
          </div>
        )}
        {data?.map((notif) => {
          const fresh = freshIds.has(notif.id);
          return (
            <DropdownMenuItem
              key={notif.id}
              onClick={() => handleClick(notif)}
              className={cn(
                "flex items-start gap-2 whitespace-normal py-2",
                DESTINATION[notif.type] ? "cursor-pointer" : "cursor-default",
                fresh && "bg-primary/5"
              )}
            >
              {fresh && <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />}
              <div className={cn(!fresh && "pl-3.5")}>
                <p className="text-sm">{notif.message}</p>
                <p className="mt-0.5 text-xs text-muted-foreground">
                  {formatShortDateTime(new Date(notif.date))}
                </p>
              </div>
            </DropdownMenuItem>
          );
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
