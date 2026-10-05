"use client";

import { useSyncExternalStore } from "react";
import { Clock, Monitor, Moon, Palette, Smartphone, Sun } from "lucide-react";
import { useTheme } from "next-themes";
import { Card, CardContent } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { InstallAppSetting } from "@/components/install-app";
import {
  AUTO_TIME_ZONE,
  TIME_ZONE_CHOICES,
  detectDeviceTimeZone,
  getTimeZonePreference,
  setTimeZonePreference,
  subscribeTimeZone,
} from "@/lib/time-zone";
import { useTimeZone } from "@/lib/use-time-zone";
import { cn, formatMatchTime } from "@/lib/utils";
import { prefersReducedMotion } from "@/lib/motion";

const THEME_OPTIONS = [
  { value: "dark", label: "Sombre", icon: Moon },
  { value: "light", label: "Clair", icon: Sun },
  { value: "system", label: "Système", icon: Monitor },
] as const;

// Le thème choisi n'est connu qu'au navigateur (localStorage de
// next-themes) : rien n'est coché au rendu serveur, pour ne pas afficher
// une sélection fausse puis la corriger à l'hydratation.
const subscribeNoop = () => () => {};

type ThemeChoice = (typeof THEME_OPTIONS)[number]["value"];

function resolveChoice(choice: ThemeChoice): "dark" | "light" {
  if (choice !== "system") return choice;
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

function ThemeSetting() {
  const { theme, setTheme } = useTheme();

  // Fondu enchaîné entre l'ancien et le nouveau thème (API View Transitions).
  // La classe est posée à la main dans le callback : next-themes ne
  // l'appliquerait qu'après coup, trop tard pour la capture du navigateur.
  function switchTheme(choice: ThemeChoice) {
    if (!document.startViewTransition || prefersReducedMotion()) {
      setTheme(choice);
      return;
    }
    const root = document.documentElement;
    root.setAttribute("data-theme-switching", "");
    const transition = document.startViewTransition(() => {
      root.classList.remove("dark", "light");
      root.classList.add(resolveChoice(choice));
      setTheme(choice);
    });
    // Transition annulée par le navigateur (onglet masqué, autre transition
    // en cours) : le thème est quand même appliqué par le callback, seul le
    // fondu saute. On évite juste l'erreur de promesse non gérée.
    transition.ready.catch(() => {});
    transition.finished.finally(() => root.removeAttribute("data-theme-switching"));
  }
  const mounted = useSyncExternalStore(subscribeNoop, () => true, () => false);

  return (
    <div className="flex flex-col gap-2">
      <p id="theme-label" className="flex items-center gap-1.5 text-sm font-medium">
        <Palette className="h-3.5 w-3.5" />
        Thème
      </p>
      <div role="radiogroup" aria-labelledby="theme-label" className="grid grid-cols-3 gap-2">
        {THEME_OPTIONS.map(({ value, label, icon: Icon }) => {
          const selected = mounted && theme === value;
          return (
            <button
              key={value}
              type="button"
              role="radio"
              aria-checked={selected}
              onClick={() => switchTheme(value)}
              className={cn(
                "flex flex-col items-center gap-1.5 rounded-xl py-3 text-sm transition-colors",
                selected ? "glass-accent" : "glass-inset-quiet text-muted-foreground"
              )}
            >
              <Icon className="h-4 w-4" />
              {label}
            </button>
          );
        })}
      </div>
      <p className="text-xs text-muted-foreground">« Système » suit le réglage clair/sombre de ton appareil.</p>
    </div>
  );
}

// Heure courante, à la minute. Côté serveur (et à l'hydratation) : null,
// sinon le rendu serveur et le navigateur divergent dès qu'une minute
// tombe entre les deux.
function subscribeMinute(onTick: () => void) {
  const id = setInterval(onTick, 15_000);
  return () => clearInterval(id);
}
const getMinute = () => Math.floor(Date.now() / 60_000);

const cityOf = (timeZone: string) => timeZone.split("/").pop()?.replace(/_/g, " ") ?? timeZone;

export function DisplaySettingsCard() {
  const preference = useSyncExternalStore(subscribeTimeZone, getTimeZonePreference, () => AUTO_TIME_ZONE);
  const timeZone = useTimeZone();
  const deviceTimeZone = useSyncExternalStore(subscribeTimeZone, detectDeviceTimeZone, () => null);
  const minute = useSyncExternalStore(subscribeMinute, getMinute, () => null);
  // Préférence enregistrée qui ne figure pas dans la liste (choisie sur une
  // ancienne version) : on l'affiche quand même plutôt qu'un select vide.
  const isListed = preference === AUTO_TIME_ZONE || TIME_ZONE_CHOICES.some((c) => c.value === preference);

  return (
    <Card>
      <CardContent className="flex flex-col gap-6 pt-6">
        <p className="text-xs uppercase tracking-wide text-muted-foreground">Affichage et application</p>

        <ThemeSetting />

        <div className="flex flex-col gap-2">
          <Label htmlFor="time-zone" className="flex items-center gap-1.5">
            <Clock className="h-3.5 w-3.5" />
            Fuseau horaire des matchs
          </Label>
          <select
            id="time-zone"
            value={preference}
            onChange={(e) => setTimeZonePreference(e.target.value)}
            className="glass-inset h-11 w-full rounded-xl border-0 bg-transparent px-3 text-base outline-none focus-visible:ring-3 focus-visible:ring-ring/50 md:text-sm [&>option]:bg-background"
          >
            <option value={AUTO_TIME_ZONE}>
              Automatique{deviceTimeZone ? ` (${cityOf(deviceTimeZone)}, d'après cet appareil)` : ""}
            </option>
            {TIME_ZONE_CHOICES.map((choice) => (
              <option key={choice.value} value={choice.value}>
                {choice.label}
              </option>
            ))}
            {!isListed && <option value={preference}>{cityOf(preference)}</option>}
          </select>
          <p className="text-xs text-muted-foreground">
            Il est actuellement <span className="font-mono text-foreground">
              {minute === null ? "--:--" : formatMatchTime(new Date(minute * 60_000), timeZone)}
            </span>{" "}
            ({cityOf(timeZone)}). Réglage propre à cet appareil.
          </p>
        </div>

        <div className="flex flex-col gap-2">
          <p className="flex items-center gap-1.5 text-sm font-medium">
            <Smartphone className="h-3.5 w-3.5" />
            Application
          </p>
          <InstallAppSetting />
        </div>
      </CardContent>
    </Card>
  );
}
