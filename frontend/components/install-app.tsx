"use client";

import { useEffect, useState, useSyncExternalStore } from "react";
import { CheckCircle2, Download, Share, SquarePlus, Smartphone } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import {
  getInstallState,
  getServerInstallState,
  isIosNonSafari,
  promptInstall,
  startInstallPromptCapture,
  subscribeInstallState,
} from "@/lib/install-prompt";
import { registerServiceWorker } from "@/lib/push";

/**
 * Monté une fois dans le layout racine : capte l'événement d'installation dès
 * le chargement et enregistre le service worker (notifications push).
 */
export function InstallPromptCapture() {
  useEffect(() => {
    startInstallPromptCapture();
    registerServiceWorker();
  }, []);
  return null;
}

function useInstallState() {
  return useSyncExternalStore(subscribeInstallState, getInstallState, getServerInstallState);
}

const IOS_STEPS = [
  {
    icon: Share,
    text: "Touche le bouton Partager de Safari (le carré avec une flèche vers le haut). Sur les iOS récents, il est dans le menu « … ».",
  },
  { icon: SquarePlus, text: "Choisis « Sur l'écran d'accueil » dans la liste." },
  { icon: CheckCircle2, text: "Touche « Ajouter » : HoopPicks s'ouvre ensuite en plein écran, comme une app." },
];

function IosInstallDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogTitle>Installer HoopPicks sur iPhone</DialogTitle>
        <DialogDescription>Trois gestes, pas de passage par l&apos;App Store.</DialogDescription>
        {isIosNonSafari() && (
          <p className="rounded-lg bg-amber-500/10 px-3 py-2 text-sm text-amber-500 light:text-amber-800">
            Sur iPhone, l&apos;ajout à l&apos;écran d&apos;accueil se fait depuis Safari : ouvre d&apos;abord cette page dans
            Safari.
          </p>
        )}
        <ol className="mt-2 flex flex-col gap-3">
          {IOS_STEPS.map(({ icon: Icon, text }, i) => (
            <li key={i} className="flex items-start gap-3">
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary/10 font-mono text-sm font-bold text-primary">
                {i + 1}
              </span>
              <span className="flex-1 pt-1 text-sm">
                <Icon className="mr-1.5 inline h-4 w-4 align-text-bottom text-muted-foreground" />
                {text}
              </span>
            </li>
          ))}
        </ol>
      </DialogContent>
    </Dialog>
  );
}

/**
 * Bouton d'installation qui s'adapte à l'appareil : vraie installation en
 * un clic quand le navigateur la propose, guide pas à pas sur iPhone, rien
 * du tout si c'est impossible ou déjà fait.
 */
export function InstallAppButton({ className }: { className?: string }) {
  const state = useInstallState();
  const [iosOpen, setIosOpen] = useState(false);

  if (state === "promptable") {
    return (
      <Button variant="outline" size="sm" className={className} onClick={() => void promptInstall()}>
        <Download className="h-4 w-4" />
        Installer l&apos;application
      </Button>
    );
  }
  if (state === "ios") {
    return (
      <>
        <Button variant="outline" size="sm" className={className} onClick={() => setIosOpen(true)}>
          <Smartphone className="h-4 w-4" />
          Ajouter à l&apos;écran d&apos;accueil
        </Button>
        <IosInstallDialog open={iosOpen} onOpenChange={setIosOpen} />
      </>
    );
  }
  return null;
}

/** Version paramètres : explique aussi pourquoi le bouton n'apparaît pas. */
export function InstallAppSetting() {
  const state = useInstallState();

  if (state === "installed") {
    return (
      <p className="flex items-center gap-2 text-sm text-muted-foreground">
        <CheckCircle2 className="h-4 w-4 text-emerald-500 light:text-emerald-800" />
        HoopPicks est installée sur cet appareil.
      </p>
    );
  }
  if (state === "unsupported") {
    return (
      <p className="text-sm text-muted-foreground">
        Ce navigateur ne propose pas l&apos;installation. Sur ordinateur, ouvre HoopPicks dans Chrome ou Edge ; sur
        iPhone, dans Safari.
      </p>
    );
  }
  return (
    <div className="flex flex-col items-start gap-3">
      <p className="text-sm text-muted-foreground">
        Ajoute HoopPicks à ton écran d&apos;accueil pour l&apos;ouvrir en plein écran, comme une vraie app.
      </p>
      <InstallAppButton />
    </div>
  );
}
