"use client";

import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { fetchProfile } from "@/lib/api/auth";

/**
 * Avant de partager une carte qui montre son maillot : s'il n'a jamais été
 * personnalisé, proposer de le faire d'abord (personne n'a envie d'envoyer
 * un maillot générique sur WhatsApp). « Partager quand même » reste possible.
 */
export function useJerseyCheck() {
  const router = useRouter();
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile, staleTime: 5 * 60 * 1000 });

  return (share: () => void) => {
    if (!profile || profile.avatarCustomized) {
      share();
      return;
    }
    toast("Ton maillot est celui par défaut", {
      description: "Personnalise-le avant de le montrer ?",
      action: {
        label: "Personnaliser",
        onClick: () => router.push(`/maillot?suite=${encodeURIComponent(window.location.pathname)}`),
      },
      cancel: { label: "Partager quand même", onClick: share },
    });
  };
}
