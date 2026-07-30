"use client";

import { useQuery } from "@tanstack/react-query";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { fetchWallet } from "@/lib/api/wallet";

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
    >
      {data.balance.toLocaleString("fr-FR")} pts
    </Badge>
  );
}