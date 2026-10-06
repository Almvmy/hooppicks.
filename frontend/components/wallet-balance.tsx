"use client";

import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchWallet } from "@/lib/api/wallet";
import { CountUp } from "@/components/motion/count-up";
import { WEEKLY_BANKROLL, formatBankrollReset } from "@/lib/utils";

export function WalletBalance() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["wallet"],
    queryFn: fetchWallet,
    staleTime: 0,
  });

  if (isLoading) return <Skeleton className="h-6 w-20 rounded-full" />;

  if (isError || !data) {
    return (
      <Badge
        variant="outline"
        className="border-destructive/30 bg-destructive/10 text-destructive"
      >
        Solde indisponible
      </Badge>
    );
  }

  return (
    <Badge
      variant="outline"
      className="border-primary/30 bg-primary/10 font-mono text-sm text-primary"
      title={`Solde de la semaine de jeu : repart à ${WEEKLY_BANKROLL.toLocaleString("fr-FR")} pts ${formatBankrollReset()}`}
    >
      {/* animateOnMount=false : la topbar est montée à chaque chargement de
          page, seul un vrai changement de solde mérite d'être animé. */}
      <CountUp value={data.balance} format={(n) => `${n.toLocaleString("fr-FR")} pts`} animateOnMount={false} />
    </Badge>
  );
}