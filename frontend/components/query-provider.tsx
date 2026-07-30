"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";

export function QueryProvider({ children }: { children: React.ReactNode }) {
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // 30s : durée par défaut pendant laquelle une donnée est
            // considérée "fraîche" et n'est pas re-fetchée automatiquement
            // au changement de page ou au retour sur l'onglet.
            staleTime: 30 * 1000,
            refetchOnWindowFocus: false,
          },
        },
      })
  );
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}