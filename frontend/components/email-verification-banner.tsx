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
    <div className="flex flex-wrap items-center justify-between gap-2 border-b border-primary/20 bg-primary/10 px-6 py-2 text-sm">
      <div className="flex items-center gap-2">
        <Mail className="h-4 w-4 shrink-0 text-primary" />
        <span>
          Vérifie ton adresse e-mail ({profile.email}) — un lien de confirmation t&apos;a été envoyé.
        </span>
      </div>
      <Button
        variant="outline"
        size="sm"
        onClick={() => mutation.mutate()}
        disabled={mutation.isPending}
      >
        {mutation.isPending ? "Envoi..." : "Renvoyer le lien"}
      </Button>
    </div>
  );
}
