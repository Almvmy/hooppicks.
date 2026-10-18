"use client";

import { useState } from "react";
import { ImageDown, Share2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";

/**
 * Partage une image générée (carte de partage) comme une photo : sur
 * téléphone, la feuille de partage propose WhatsApp, les statuts, etc. Sans
 * partage de fichiers (ordinateur), l'image est téléchargée.
 */
/** `discreet` : simple icône, pour un bouton posé sur un visuel (podium) sans lui voler la vedette. */
export function ShareImageButton({ src, fileName, text, discreet = false, label = "Partager l'image" }: { src: string; fileName: string; text: string; discreet?: boolean; label?: string }) {
  const [busy, setBusy] = useState(false);

  async function share() {
    setBusy(true);
    try {
      const res = await fetch(src, { cache: "no-store" });
      if (!res.ok) throw new Error();
      const blob = await res.blob();
      const file = new File([blob], fileName, { type: "image/png" });
      if (navigator.canShare?.({ files: [file] })) {
        try {
          await navigator.share({ files: [file], text });
        } catch (e) {
          if (!(e instanceof DOMException && e.name === "AbortError")) throw e;
        }
        return;
      }
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = fileName;
      a.click();
      URL.revokeObjectURL(url);
      toast.success("Image téléchargée : envoie-la où tu veux.");
    } catch {
      toast.error("Impossible de préparer l'image. Réessaie.");
    } finally {
      setBusy(false);
    }
  }

  if (discreet) {
    return (
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        className="text-muted-foreground hover:text-foreground"
        disabled={busy}
        onClick={share}
        aria-label="Partager l'image"
        title="Partager l'image"
      >
        <Share2 className={busy ? "animate-pulse" : undefined} />
      </Button>
    );
  }

  return (
    <Button type="button" variant="lit" size="sm" className="gap-1.5" disabled={busy} onClick={share}>
      <ImageDown className="h-3.5 w-3.5" />
      {busy ? "Préparation…" : label}
    </Button>
  );
}
