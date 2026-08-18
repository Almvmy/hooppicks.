"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui/button";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Journalisation console pour le debug ; à remplacer par un outil de
    // suivi d'erreurs (Sentry, etc.) le jour où l'app en a un.
    console.error(error);
  }, [error]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-background px-4 text-center">
      <span className="font-mono text-sm text-destructive">Erreur inattendue</span>
      <h1 className="font-heading text-3xl font-bold tracking-tight sm:text-4xl">
        Un imprévu sur le parquet
      </h1>
      <p className="max-w-md text-muted-foreground">
        Quelque chose s&apos;est mal passé de notre côté. Réessaie, et si ça
        persiste, reviens un peu plus tard.
      </p>
      <Button size="lg" className="mt-2" onClick={() => reset()}>
        Réessayer
      </Button>
    </div>
  );
}
