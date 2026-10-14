"use client";

import { useEffect, useRef, useState } from "react";
import Image from "next/image";
import { Pause, Play } from "lucide-react";
import { cn } from "@/lib/utils";

export interface FeatureSlide {
  id: string;
  title: string;
  description: string;
  /** Capture de l'app au format iPhone (390×797 pt, sans la barre d'état que dessine le cadre). */
  image: string;
  alt: string;
}

const SLIDE_MS = 4500;

/**
 * Vitrine de l'app : un iPhone dont l'écran fait défiler les captures, et la
 * liste des fonctionnalités qui suit (à côté sur ordinateur, dessous sur
 * mobile). Défilement automatique piloté par l'animation de la barre de
 * progression (son « animationend » passe à la suivante) : pause et reprise
 * restent synchronisées avec ce qu'on voit, sans minuterie à recaler.
 * En pause au survol, hors de l'écran, onglet caché, après un geste de
 * l'utilisateur ; jamais automatique si l'appareil demande moins d'animations.
 */
export function FeatureCarousel({ slides }: { slides: FeatureSlide[] }) {
  const rootRef = useRef<HTMLDivElement>(null);
  const [active, setActive] = useState(0);
  const [hovered, setHovered] = useState(false);
  const [visible, setVisible] = useState(false);
  const [userPaused, setUserPaused] = useState(false);
  const [reducedMotion, setReducedMotion] = useState(false);
  const touchX = useRef<number | null>(null);

  useEffect(() => {
    const media = window.matchMedia("(prefers-reduced-motion: reduce)");
    const sync = () => setReducedMotion(media.matches);
    sync();
    media.addEventListener("change", sync);

    const root = rootRef.current;
    const observer = new IntersectionObserver(([entry]) => setVisible(entry.isIntersecting && !document.hidden), { threshold: 0.35 });
    if (root) observer.observe(root);
    const onVisibility = () => document.hidden && setVisible(false);
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      media.removeEventListener("change", sync);
      observer.disconnect();
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, []);

  const playing = visible && !hovered && !userPaused && !reducedMotion;
  const go = (index: number) => setActive((index + slides.length) % slides.length);

  function onTouchEnd(x: number) {
    if (touchX.current === null) return;
    const dx = x - touchX.current;
    touchX.current = null;
    if (Math.abs(dx) < 40) return;
    go(active + (dx < 0 ? 1 : -1));
  }

  // Barre de progression : même durée et même état (pause/lecture) partout.
  const progressStyle = {
    "--hp-slide-ms": `${SLIDE_MS}ms`,
    animationPlayState: playing ? "running" : "paused",
  } as React.CSSProperties;

  const current = slides[active];

  return (
    <div
      ref={rootRef}
      className="mx-auto grid max-w-5xl items-center gap-10 px-4 lg:grid-cols-[1fr_auto] lg:gap-20"
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      aria-roledescription="carrousel"
      aria-label="Aperçu de l'application"
    >
      {/* Moteur du défilement : invisible, toujours rendu (les barres visibles peuvent être masquées selon l'écran). */}
      <div key={`driver-${active}`} aria-hidden className="hp-progress pointer-events-none fixed h-px w-px opacity-0" style={progressStyle} onAnimationEnd={() => go(active + 1)} />

      {/* Ordinateur : la liste des fonctionnalités, la courante dépliée avec sa progression */}
      <ol className="order-2 hidden flex-col gap-1 lg:order-1 lg:flex" role="tablist" aria-label="Fonctionnalités">
        {slides.map((slide, index) => {
          const isActive = index === active;
          return (
            <li key={slide.id}>
              <button
                type="button"
                role="tab"
                aria-selected={isActive}
                onClick={() => go(index)}
                className={cn(
                  "group relative w-full overflow-hidden rounded-2xl px-5 py-4 text-left transition-colors duration-300",
                  isActive ? "glass" : "hover:bg-tint/[0.03]"
                )}
              >
                <span className="flex items-center gap-3">
                  <span className={cn("font-mono text-xs tabular-nums", isActive ? "text-primary" : "text-muted-foreground")}>
                    {String(index + 1).padStart(2, "0")}
                  </span>
                  <span className={cn("font-heading text-lg font-bold transition-colors", !isActive && "text-muted-foreground group-hover:text-foreground")}>
                    {slide.title}
                  </span>
                </span>
                <span
                  className={cn(
                    "grid transition-[grid-template-rows,opacity] duration-500",
                    isActive ? "grid-rows-[1fr] opacity-100" : "grid-rows-[0fr] opacity-0"
                  )}
                >
                  <span className="overflow-hidden">
                    <span className="block pl-8 pt-1.5 text-sm text-muted-foreground">{slide.description}</span>
                  </span>
                </span>
                {isActive && (
                  <span className="absolute inset-x-5 bottom-0 h-0.5 overflow-hidden rounded-full bg-tint/10">
                    <span key={`bar-${active}`} className="hp-progress block h-full rounded-full bg-primary" style={progressStyle} />
                  </span>
                )}
              </button>
            </li>
          );
        })}
      </ol>

      <div className="order-1 flex flex-col items-center gap-7 lg:order-2">
        <PhoneMockup>
          <div
            className="flex h-full transition-transform duration-700 [transition-timing-function:cubic-bezier(0.22,1,0.36,1)] motion-reduce:transition-none"
            style={{ transform: `translateX(-${active * 100}%)` }}
            onTouchStart={(e) => (touchX.current = e.touches[0].clientX)}
            onTouchEnd={(e) => onTouchEnd(e.changedTouches[0].clientX)}
          >
            {slides.map((slide, index) => (
              <div key={slide.id} className="relative h-full w-full shrink-0" aria-hidden={index !== active}>
                <Image
                  src={slide.image}
                  alt={slide.alt}
                  fill
                  sizes="300px"
                  priority={index === 0}
                  className="object-cover object-top"
                  draggable={false}
                />
              </div>
            ))}
          </div>
        </PhoneMockup>

        {/* Mobile : texte de la fonctionnalité courante, sous le téléphone */}
        <div className="flex min-h-24 max-w-sm flex-col items-center text-center lg:hidden" aria-live={playing ? "off" : "polite"}>
          <h3 key={`t-${current.id}`} className="font-heading text-xl font-bold animate-in fade-in slide-in-from-bottom-1 duration-500">
            {current.title}
          </h3>
          <p key={`d-${current.id}`} className="mt-1.5 text-sm text-muted-foreground animate-in fade-in duration-700">
            {current.description}
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5 lg:hidden" role="tablist" aria-label="Fonctionnalités">
            {slides.map((slide, index) => (
              <button
                key={slide.id}
                type="button"
                role="tab"
                aria-selected={index === active}
                aria-label={slide.title}
                onClick={() => go(index)}
                className={cn(
                  "relative h-1.5 overflow-hidden rounded-full bg-tint/15 transition-all duration-500",
                  index === active ? "w-8" : "w-1.5"
                )}
              >
                {index === active && <span key={`dot-${active}`} className="hp-progress absolute inset-0 rounded-full bg-primary" style={progressStyle} />}
              </button>
            ))}
          </div>
          {!reducedMotion && (
            <button
              type="button"
              onClick={() => setUserPaused((p) => !p)}
              aria-label={userPaused ? "Reprendre le défilement" : "Mettre le défilement en pause"}
              className="flex h-7 w-7 items-center justify-center rounded-full text-muted-foreground transition-colors hover:text-foreground"
            >
              {userPaused ? <Play className="h-3.5 w-3.5" /> : <Pause className="h-3.5 w-3.5" />}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

/** iPhone (proportions 390×844) : cadre titane, Dynamic Island, barre d'état, boutons sur les tranches. */
function PhoneMockup({ children }: { children: React.ReactNode }) {
  return (
    <div className="relative">
      {/* halo orange derrière le téléphone */}
      <div aria-hidden className="absolute left-1/2 top-1/2 h-[70%] w-[120%] -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary/20 blur-3xl" />

      {/* boutons latéraux */}
      <span aria-hidden className="absolute -left-[3px] top-[18%] h-7 w-[3px] rounded-l-sm bg-[#3a404c]" />
      <span aria-hidden className="absolute -left-[3px] top-[25%] h-12 w-[3px] rounded-l-sm bg-[#3a404c]" />
      <span aria-hidden className="absolute -left-[3px] top-[34%] h-12 w-[3px] rounded-l-sm bg-[#3a404c]" />
      <span aria-hidden className="absolute -right-[3px] top-[28%] h-16 w-[3px] rounded-r-sm bg-[#3a404c]" />

      <div className="relative w-[272px] rounded-[3rem] bg-gradient-to-br from-[#4a505c] via-[#22262e] to-[#3b414c] p-[3px] shadow-[0_40px_80px_-20px_rgba(0,0,0,0.75),0_0_0_1px_rgba(255,255,255,0.06)] sm:w-[300px]">
        <div className="rounded-[2.85rem] bg-black p-[9px]">
          <div className="relative aspect-[390/844] overflow-hidden rounded-[2.35rem] bg-[#0B1120]">
            {/* barre d'état (l'app installée la peint en marine) */}
            <div aria-hidden className="absolute inset-x-0 top-0 z-20 flex h-[5.57%] items-end justify-between px-[9%] pb-[1%] text-[11px] font-semibold text-white sm:text-xs">
              <span className="tracking-tight">9:41</span>
              <span className="flex items-center gap-1">
                <svg width="15" height="10" viewBox="0 0 17 11" fill="currentColor"><rect x="0" y="7" width="3" height="4" rx="0.8" /><rect x="4.5" y="5" width="3" height="6" rx="0.8" /><rect x="9" y="2.5" width="3" height="8.5" rx="0.8" /><rect x="13.5" y="0" width="3" height="11" rx="0.8" /></svg>
                <svg width="14" height="10" viewBox="0 0 16 11" fill="currentColor"><path d="M8 2.2c2.2 0 4.2.9 5.7 2.3l1.1-1.1A9.6 9.6 0 0 0 8 .6 9.6 9.6 0 0 0 1.2 3.4l1.1 1.1A8 8 0 0 1 8 2.2Zm0 3.2c1.3 0 2.5.5 3.4 1.4l1.1-1.1A6.4 6.4 0 0 0 8 3.8c-1.8 0-3.4.7-4.5 1.9l1.1 1.1c.9-.9 2.1-1.4 3.4-1.4Zm0 3.2c-.5 0-.9.2-1.2.5L8 10.4l1.2-1.3c-.3-.3-.7-.5-1.2-.5Z" /></svg>
                <svg width="24" height="11" viewBox="0 0 25 12" fill="none"><rect x="0.5" y="0.5" width="21" height="11" rx="3" stroke="currentColor" strokeOpacity="0.4" /><rect x="2" y="2" width="16" height="8" rx="1.8" fill="currentColor" /><path d="M23 4v4c.7-.3 1.2-1 1.2-2s-.5-1.7-1.2-2Z" fill="currentColor" fillOpacity="0.4" /></svg>
              </span>
            </div>
            {/* Dynamic Island */}
            <div aria-hidden className="absolute left-1/2 top-[1.3%] z-30 h-[3.5%] w-[31%] -translate-x-1/2 rounded-full bg-black" />

            <div className="absolute inset-x-0 bottom-0 top-[5.57%] overflow-hidden">{children}</div>

            {/* barre d'accueil iOS */}
            <div aria-hidden className="absolute bottom-[0.9%] left-1/2 z-20 h-[4px] w-[34%] -translate-x-1/2 rounded-full bg-white/70" />
            {/* reflet de la vitre */}
            <div aria-hidden className="pointer-events-none absolute inset-0 z-10 bg-[linear-gradient(115deg,rgba(255,255,255,0.07)_0%,rgba(255,255,255,0)_38%)]" />
          </div>
        </div>
      </div>
    </div>
  );
}
