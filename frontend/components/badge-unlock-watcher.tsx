"use client";

import { createElement, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { fetchBadges } from "@/lib/api/badges";
import { badgeIcon } from "@/lib/badges";
import { detectNewlyUnlockedBadges } from "@/lib/badge-notifications";
import { ConfettiBurst } from "@/components/confetti-burst";
import type { UserBadge } from "@/lib/types";

/**
 * Composant invisible : surveille en arrière-plan les badges de l'utilisateur
 * et déclenche confettis + toast de félicitations pour chaque nouveau badge
 * débloqué depuis la dernière visite. À monter une seule fois, dans AppShell.
 */
export function BadgeUnlockWatcher() {
  const [celebrating, setCelebrating] = useState(false);
  const [lastChecked, setLastChecked] = useState<UserBadge[] | undefined>(undefined);

  const { data: badges } = useQuery({
    queryKey: ["badges"],
    queryFn: fetchBadges,
    staleTime: 60 * 1000,
  });

  // Ajustement pendant le rendu, pas dans un effet (setState synchrone dans
  // un effet = rendus en cascade) : même technique que BetResultWatcher.
  if (badges && badges !== lastChecked) {
    setLastChecked(badges);
    const newlyUnlocked = detectNewlyUnlockedBadges(badges);

    if (newlyUnlocked.length > 0) {
      setCelebrating(true);
    }

    for (const badge of newlyUnlocked) {
      toast.success(`Badge débloqué : ${badge.label}`, {
        description: badge.description,
        icon: createElement(badgeIcon(badge.icon), { className: "h-4 w-4 text-primary" }),
      });
    }
  }

  return celebrating ? <ConfettiBurst onDone={() => setCelebrating(false)} /> : null;
}
