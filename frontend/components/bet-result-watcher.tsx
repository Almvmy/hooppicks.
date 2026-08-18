"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { PartyPopper } from "lucide-react";
import { fetchBets } from "@/lib/api/bets";
import { detectNewlyWonBets } from "@/lib/bet-notifications";
import { ConfettiBurst } from "@/components/confetti-burst";
import { PlacedBet } from "@/lib/types";

/**
 * Composant invisible : surveille en arrière-plan les paris de l'utilisateur
 * et déclenche confettis + toast "swish" dès qu'un ticket passe à "Gagné"
 * depuis la dernière visite. À monter une seule fois, dans AppShell.
 */
export function BetResultWatcher() {
  const [celebrating, setCelebrating] = useState(false);
  const [lastChecked, setLastChecked] = useState<PlacedBet[] | undefined>(undefined);

  const { data: bets } = useQuery({
    queryKey: ["bets"],
    queryFn: fetchBets,
    refetchInterval: 60 * 1000, // vérifie les résultats résolus en arrière-plan
  });

  // Ajustement pendant le rendu (pas dans un effet) : dès qu'un nouveau
  // snapshot de paris arrive de React Query, on vérifie les victoires pas
  // encore fêtées. Cf. la même technique utilisée dans mobile-nav.tsx.
  if (bets && bets !== lastChecked) {
    setLastChecked(bets);
    const newlyWon = detectNewlyWonBets(bets);

    if (newlyWon.length > 0) {
      setCelebrating(true);
      const total = newlyWon.reduce((sum, b) => sum + b.potentialPayout, 0);
      toast.success(
        newlyWon.length === 1 ? "Ticket gagnant !" : `${newlyWon.length} tickets gagnants !`,
        {
          description: `+${total.toLocaleString("fr-FR")} pts au total. Swish !`,
          icon: <PartyPopper className="h-4 w-4 text-primary" />,
        }
      );
    }
  }

  return celebrating ? <ConfettiBurst onDone={() => setCelebrating(false)} /> : null;
}
