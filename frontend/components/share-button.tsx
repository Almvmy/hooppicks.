"use client";

import { Share2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/**
 * Partage natif du téléphone (WhatsApp, Messenger…) s'il existe, sinon
 * ouverture directe de WhatsApp, le canal principal des joueurs. Le lien
 * pointe vers une page publique dont l'aperçu est une image générée.
 */
export function ShareButton({
  path,
  text,
  label = "Partager",
  className,
  size = "sm",
}: {
  path: string;
  text: string;
  label?: string;
  className?: string;
  size?: "sm" | "default";
}) {
  async function share() {
    const url = `${window.location.origin}${path}`;
    if (navigator.share) {
      try {
        await navigator.share({ text, url });
        return;
      } catch (e) {
        // Partage annulé par l'utilisateur : rien à faire.
        if (e instanceof DOMException && e.name === "AbortError") return;
      }
    }
    const opened = window.open(`https://wa.me/?text=${encodeURIComponent(`${text} ${url}`)}`, "_blank", "noopener");
    if (!opened) {
      await navigator.clipboard?.writeText(url).catch(() => undefined);
      toast.success("Lien copié.");
    }
  }

  return (
    <Button type="button" variant="outline" size={size} className={cn("gap-1.5", className)} onClick={share}>
      <Share2 className="h-3.5 w-3.5" />
      {label}
    </Button>
  );
}
