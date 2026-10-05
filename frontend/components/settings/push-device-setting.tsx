"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BellRing } from "lucide-react";
import { toast } from "sonner";
import { Switch } from "@/components/ui/switch";
import { disablePush, enablePush, getPushState, PushState } from "@/lib/push";

const HINTS: Record<PushState, string> = {
  enabled: "Tu reçois les notifications sur cet appareil, même app fermée.",
  disabled: "Reçois tes résultats et les coups d'envoi même quand l'app est fermée.",
  denied:
    "Notifications bloquées pour HoopPicks dans ce navigateur : autorise-les dans les réglages du site, puis reviens ici.",
  "needs-install":
    "Sur iPhone, les notifications ne marchent que dans l'app installée : ajoute HoopPicks à ton écran d'accueil (section Application ci-dessous), puis active-les depuis l'app.",
  unsupported: "Ce navigateur ne gère pas les notifications push.",
  unavailable: "Notifications push indisponibles pour le moment.",
};

/** Interrupteur par appareil : complète les préférences du compte (quoi notifier) par le canal (où). */
export function PushDeviceSetting() {
  const queryClient = useQueryClient();
  const { data: state, isLoading, isError } = useQuery({
    queryKey: ["push-state"],
    queryFn: getPushState,
    staleTime: Infinity,
  });

  const mutation = useMutation({
    mutationFn: (enable: boolean) => (enable ? enablePush() : disablePush()),
    onSuccess: (next) => {
      queryClient.setQueryData(["push-state"], next);
      if (next === "enabled") toast.success("Notifications activées sur cet appareil.");
      if (next === "disabled") toast.success("Notifications désactivées sur cet appareil.");
    },
    onError: () => toast.error("Impossible de modifier les notifications. Réessaie."),
  });

  const canToggle = state === "enabled" || state === "disabled";

  return (
    <div className="flex items-center justify-between gap-4">
      <div>
        <p className="flex items-center gap-1.5 text-sm font-medium">
          <BellRing className="h-3.5 w-3.5" />
          Sur cet appareil
        </p>
        <p className="text-xs text-muted-foreground">
          {isLoading ? "Vérification…" : isError || !state ? HINTS.unsupported : HINTS[state]}
        </p>
      </div>
      {canToggle && (
        <Switch
          aria-label="Notifications push sur cet appareil"
          checked={state === "enabled"}
          onCheckedChange={(checked) => mutation.mutate(checked)}
          disabled={mutation.isPending}
        />
      )}
    </div>
  );
}
