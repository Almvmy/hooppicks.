"use client";

import { Mail } from "lucide-react";
import { toast } from "sonner";
import { useMutation, useQuery } from "@tanstack/react-query";
import { fetchProfile, resendVerificationEmail } from "@/lib/api/auth";
import { Button } from "@/components/ui/button";

export function EmailVerificationBanner() {
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });

  const mutation = useMutation({
    mutationFn: resendVerificationEmail,
    onSuccess: () => toast.success("E-mail de confirmation renvoyé."),
    onError: () => toast.error("Impossible d'envoyer l'e-mail. Réessaie plus tard."),
  });

  if (!profile || profile.emailVerified) return null;

  return (
    // Une ligne sur mobile : le rappel prenait un quart du premier écran.
    <div className="flex items-center justify-between gap-2 border-b border-primary/20 bg-primary/10 px-4 py-1.5 text-xs sm:px-6 sm:py-2 sm:text-sm">
      <div className="flex min-w-0 items-center gap-2">
        <Mail className="h-4 w-4 shrink-0 text-primary" />
        <span className="truncate sm:whitespace-normal">
          <span className="sm:hidden">Confirme ton e-mail : lien envoyé.</span>
          <span className="hidden sm:inline">
            Vérifie ton adresse e-mail ({profile.email}), un lien de confirmation t&apos;a été envoyé.
          </span>
        </span>
      </div>
      <Button
        variant="outline"
        size="sm"
        className="h-7 shrink-0 px-2 text-xs sm:h-8 sm:px-3 sm:text-sm"
        onClick={() => mutation.mutate()}
        disabled={mutation.isPending}
      >
        {mutation.isPending ? "Envoi..." : "Renvoyer le lien"}
      </Button>
    </div>
  );
}
