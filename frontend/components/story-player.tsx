"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { X } from "lucide-react";
import { cn } from "@/lib/utils";

const SLIDE_MS = 7000;

export type Slide = { key: string; tint: string; render: () => React.ReactNode };

export function Eyebrow({ children }: { children: React.ReactNode }) {
  return <p className="text-xs font-bold uppercase tracking-[0.2em] text-brand">{children}</p>;
}

export function Big({ children, className }: { children: React.ReactNode; className?: string }) {
  return <h2 className={cn("font-heading text-4xl font-black italic uppercase leading-[0.95] [overflow-wrap:anywhere] sm:text-5xl", className)}>{children}</h2>;
}

/** Entrée décalée des éléments d'une diapo (relancée à chaque diapo par sa clé). */
export function Reveal({ delay = 0, children, className }: { delay?: number; children: React.ReactNode; className?: string }) {
  return (
    <div
      className={cn("min-w-0 animate-in fade-in slide-in-from-bottom-4 duration-700", className)}
      style={{ animationDelay: `${delay}ms`, animationFillMode: "both" }}
    >
      {children}
    </div>
  );
}

/**
 * Écrans plein écran façon « story » : avance seule, touche à droite ou à
 * gauche pour naviguer, appui long pour mettre en pause.
 */
export function StoryPlayer({
  open,
  onClose,
  slides,
  label,
  header,
}: {
  open: boolean;
  onClose: () => void;
  slides: Slide[];
  /** Nom pour les lecteurs d'écran. */
  label: string;
  /** Petit texte en haut de chaque écran. */
  header: string;
}) {
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const [elapsed, setElapsed] = useState(0);
  const last = slides.length - 1;
  const current = slides[Math.min(index, last)];
  const holdTimer = useRef<number | null>(null);
  const held = useRef(false);

  // Jamais onClose() dans une fonction de mise à jour d'état : elle tourne
  // pendant le rendu, et fermer met à jour le composant parent.
  const go = useCallback(
    (delta: number) => {
      setElapsed(0);
      if (index + delta > last) {
        onClose();
        return;
      }
      setIndex(Math.max(0, index + delta));
    },
    [index, last, onClose]
  );

  // Avance seule : une horloge commune, mise en pause à l'appui long.
  useEffect(() => {
    if (!open || paused) return;
    const step = 100;
    const id = window.setInterval(() => {
      setElapsed((e) => {
        if (e + step >= SLIDE_MS) {
          if (index < last) {
            setIndex((i) => i + 1);
            return 0;
          }
          return SLIDE_MS;
        }
        return e + step;
      });
    }, step);
    return () => window.clearInterval(id);
  }, [open, paused, index, last]);

  useEffect(() => {
    if (!open) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
      if (e.key === "ArrowRight") go(1);
      if (e.key === "ArrowLeft") go(-1);
    }
    window.addEventListener("keydown", onKey);
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = overflow;
    };
  }, [open, go, onClose]);

  if (!open || !current) return null;

  function onPointerDown() {
    held.current = false;
    holdTimer.current = window.setTimeout(() => {
      held.current = true;
      setPaused(true);
    }, 250);
  }

  function onPointerUp(e: React.PointerEvent<HTMLDivElement>) {
    if (holdTimer.current) window.clearTimeout(holdTimer.current);
    if (held.current) {
      setPaused(false);
      return;
    }
    // Un lien ou un bouton de la diapo garde son clic.
    if ((e.target as HTMLElement).closest("a, button")) return;
    const rect = e.currentTarget.getBoundingClientRect();
    go(e.clientX - rect.left < rect.width * 0.3 ? -1 : 1);
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={label}
      className="island fixed inset-0 z-[95] flex items-stretch justify-center bg-black/80 backdrop-blur-sm"
    >
      <div className="relative flex h-full w-full max-w-md flex-col overflow-hidden bg-jersey text-jersey-foreground sm:my-6 sm:h-auto sm:rounded-3xl sm:shadow-[var(--lift-strong)]">
        <div
          key={`bg-${current.key}`}
          aria-hidden
          className="pointer-events-none absolute inset-0 animate-in fade-in duration-700"
          style={{
            background: `radial-gradient(90% 60% at 100% 0%, color-mix(in srgb, ${current.tint} 45%, transparent), transparent 70%), radial-gradient(70% 50% at 0% 100%, color-mix(in srgb, var(--paint) 30%, transparent), transparent 70%)`,
          }}
        />
        <div aria-hidden className="pointer-events-none absolute -bottom-28 -right-28 h-80 w-80 rounded-full border-[6px] border-white/[0.06]" />

        <div className="relative z-10 flex gap-1 px-4 pt-[max(1rem,env(safe-area-inset-top))]">
          {slides.map((s, i) => (
            <div key={s.key} className="h-1 flex-1 overflow-hidden rounded-full bg-white/20">
              <div
                className="h-full rounded-full bg-white"
                style={{ width: i < index ? "100%" : i === index ? `${(elapsed / SLIDE_MS) * 100}%` : "0%" }}
              />
            </div>
          ))}
        </div>
        <div className="relative z-10 flex items-center justify-between px-4 pt-3">
          <span className="text-xs font-semibold text-jersey-muted">{header}</span>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fermer"
            className="rounded-full p-1.5 text-jersey-foreground/80 transition-colors hover:bg-white/10 hover:text-jersey-foreground"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div
          key={current.key}
          className="relative z-0 flex min-h-[70vh] min-w-0 flex-1 select-none flex-col px-6 pb-[max(2rem,env(safe-area-inset-bottom))] pt-4 sm:min-h-[620px]"
          onPointerDown={onPointerDown}
          onPointerUp={onPointerUp}
          onPointerLeave={() => {
            if (holdTimer.current) window.clearTimeout(holdTimer.current);
            if (held.current) setPaused(false);
          }}
        >
          {current.render()}
          <p className="mt-6 text-center text-[11px] text-jersey-muted">
            {index < last ? "Touche à droite pour avancer, à gauche pour revenir" : "Touche pour fermer"}
          </p>
        </div>
      </div>
    </div>
  );
}
