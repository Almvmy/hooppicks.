"use client";

import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { Trophy } from "lucide-react";
import { fetchBadges } from "@/lib/api/badges";
import { detectNewlyUnlockedBadges } from "@/lib/badge-notifications";

/**
 * Composant invisible : surveille en arrière-plan les badges de l'utilisateur
 * et affiche un toast de félicitations pour chaque nouveau badge débloqué
 * depuis la dernière visite. À monter une seule fois, dans AppShell.
 */
export function BadgeUnlockWatcher() {
  const { data: badges } = useQuery({
    queryKey: ["badges"],
    queryFn: fetchBadges,
    staleTime: 60 * 1000,
  });

  useEffect(() => {
    if (!badges) return;
    const newlyUnlocked = detectNewlyUnlockedBadges(badges);

    for (const badge of newlyUnlocked) {
      toast.success(`Badge débloqué : ${badge.label}`, {
        description: badge.description,
        icon: <Trophy className="h-4 w-4 text-primary" />,
      });
    }
  }, [badges]);

  return null;
}