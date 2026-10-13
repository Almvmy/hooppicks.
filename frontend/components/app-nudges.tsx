"use client";

import { useEffect, useState } from "react";
import { QueryClient, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BellRing, Smartphone, X } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { IosInstallDialog, useInstallState } from "@/components/install-app";
import { isStandalone, promptInstall } from "@/lib/install-prompt";
import { canNudge, recordNudge, useNudge } from "@/lib/nudge";
import { enablePush, getPushState, PushState } from "@/lib/push";

function usePushState() {
  return useQuery({ queryKey: ["push-state"], queryFn: getPushState, staleTime: Infinity });
}

async function activatePush(queryClient: QueryClient) {
  let next: PushState;
  try {
    next = await enablePush();
  } catch {
    toast.error("Impossible d'activer les notifications. Réessaie depuis Paramètres.");
    return;
  }
  queryClient.setQueryData(["push-state"], next);
  if (next === "enabled") toast.success("Notifications activées sur cet appareil.");
  if (next === "denied") toast.error("Notifications bloquées : autorise-les dans les réglages du navigateur.");
}

function NudgeCard({
  icon: Icon,
  title,
  text,
  action,
  onDismiss,
}: {
  icon: React.ElementType;
  title: string;
  text: string;
  action: React.ReactNode;
  onDismiss: () => void;
}) {
  return (
    <div className="glass-accent relative flex items-start gap-3 rounded-2xl px-4 py-3">
      <Icon className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
      <div className="min-w-0 flex-1 pr-6">
        <p className="font-semibold">{title}</p>
        <p className="text-xs text-muted-foreground">{text}</p>
        <div className="mt-2">{action}</div>
      </div>
      <button
        type="button"
        onClick={onDismiss}
        aria-label="Plus tard"
        className="absolute right-2 top-2 flex h-8 w-8 items-center justify-center rounded-full text-muted-foreground hover:text-foreground"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}

// Sur téléphone seulement : sur ordinateur, l'app installée n'apporte presque rien.
function useInstallNudge(): [boolean, () => void] {
  const state = useInstallState();
  const [visible, dismiss] = useNudge("install");
  const eligible =
    visible && (state === "promptable" || state === "ios") && window.matchMedia("(pointer: coarse)").matches;
  return [eligible, dismiss];
}

function InstallNudgeCard({ onDismiss }: { onDismiss: () => void }) {
  const state = useInstallState();
  const [iosOpen, setIosOpen] = useState(false);

  return (
    <>
      <NudgeCard
        icon={Smartphone}
        title="Mets HoopPicks sur ton écran d'accueil"
        text="Plein écran, ouverture en un geste, et tes résultats en notification."
        onDismiss={onDismiss}
        action={
          state === "promptable" ? (
            <Button size="sm" onClick={() => void promptInstall()}>
              Installer
            </Button>
          ) : (
            <Button size="sm" onClick={() => setIosOpen(true)}>
              Comment faire
            </Button>
          )
        }
      />
      <IosInstallDialog open={iosOpen} onOpenChange={setIosOpen} />
    </>
  );
}

function PushNudgeCard({ hasBets }: { hasBets: boolean }) {
  const queryClient = useQueryClient();
  const { data: state } = usePushState();
  const [visible, dismiss] = useNudge("push");
  const mutation = useMutation({ mutationFn: () => activatePush(queryClient) });
  if (!visible || state !== "disabled") return null;
  // Premier lancement de l'app installée : sur iPhone, c'est le seul endroit
  // où le push existe, et le joueur vient justement de faire l'effort.
  const installed = isStandalone();
  if (!installed && !hasBets) return null;

  return (
    <NudgeCard
      icon={BellRing}
      title={installed ? "Bienvenue dans l'app !" : "Ne rate plus aucun résultat"}
      text="Active les notifications : résultats de tes tickets et matchs suivis, même app fermée."
      onDismiss={dismiss}
      action={
        <Button size="sm" onClick={() => mutation.mutate()} disabled={mutation.isPending}>
          Activer les notifications
        </Button>
      }
    />
  );
}

/** Une invitation à la fois sur l'accueil : d'abord installer, ensuite les notifications. */
export function AppNudges({ hasBets }: { hasBets: boolean }) {
  const [showInstall, dismissInstall] = useInstallNudge();
  return showInstall ? <InstallNudgeCard onDismiss={dismissInstall} /> : <PushNudgeCard hasBets={hasBets} />;
}

/**
 * Après un geste qui appelle un résultat (pari validé, match suivi) : le
 * moment où l'intérêt des notifications est évident. Renvoie false si rien
 * n'a été montré, pour que l'appelant affiche sa confirmation habituelle.
 */
export async function offerPushNotifications(
  queryClient: QueryClient,
  { title, description, iosHint }: { title: string; description: string; iosHint?: boolean }
): Promise<boolean> {
  if (!canNudge("push-toast")) return false;
  const state = await queryClient
    .fetchQuery({ queryKey: ["push-state"], queryFn: getPushState, staleTime: Infinity })
    .catch(() => null);

  if (state === "disabled") {
    recordNudge("push-toast");
    toast.success(title, {
      description,
      duration: 10000,
      action: { label: "Activer", onClick: () => void activatePush(queryClient) },
    });
    return true;
  }
  if (state === "needs-install" && iosHint) {
    recordNudge("push-toast");
    toast.success(title, {
      description: "Sur iPhone, les notifications n'arrivent que dans l'app ajoutée à l'écran d'accueil (Paramètres › Application).",
      duration: 10000,
    });
    return true;
  }
  return false;
}

/** Signale une fois par appareil que l'app installée a été ouverte (chiffre de la console admin). */
export function AppInstallReporter() {
  useEffect(() => {
    if (!isStandalone()) return;
    try {
      if (window.localStorage.getItem("app-installed-reported") === "1") return;
    } catch {
      return;
    }
    // fetch direct, pas apiFetch : un 401 ici ne doit pas renvoyer à la connexion.
    void fetch("/api/auth/app-installed", { method: "POST", credentials: "include" }).then((res) => {
      if (!res.ok) return;
      try {
        window.localStorage.setItem("app-installed-reported", "1");
      } catch {
        // Stockage indisponible : on resignalera, le serveur ne garde que la première fois.
      }
    }, () => undefined);
  }, []);
  return null;
}
