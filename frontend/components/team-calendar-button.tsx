"use client";

import { CalendarPlus } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * Ajoute les matchs d'une équipe à l'agenda du téléphone : webcal:// ouvre
 * l'abonnement (mis à jour tout seul), le lien direct télécharge le fichier
 * pour les agendas qui ne savent pas s'abonner.
 */
export function TeamCalendarButton({ abbreviation, className }: { abbreviation: string; className?: string }) {
  const path = `/calendrier/${abbreviation}.ics`;

  function subscribe() {
    window.location.href = `webcal://${window.location.host}${path}`;
  }

  return (
    <span className={cn("inline-flex flex-wrap items-center gap-x-3 gap-y-1", className)}>
      <button
        type="button"
        onClick={subscribe}
        className="inline-flex items-center gap-1.5 rounded-lg bg-primary/10 px-3 py-1.5 text-sm font-semibold text-primary transition-colors hover:bg-primary/15"
      >
        <CalendarPlus className="h-4 w-4" />
        Ajouter ses matchs à mon agenda
      </button>
      <a href={path} download className="text-xs text-muted-foreground underline-offset-2 hover:underline">
        ou télécharger le fichier
      </a>
    </span>
  );
}
