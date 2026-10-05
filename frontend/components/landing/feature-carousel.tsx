"use client";

import { useEffect, useRef, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";

export interface FeatureSlide {
  id: string;
  title: string;
  description: string;
  preview: React.ReactNode;
}

/**
 * Carrousel à défilement natif (scroll-snap) : swipe au doigt, trackpad et
 * clavier gratuits, sans réimplémenter la physique du glissé en JS. Le JS
 * ne fait qu'observer quelle carte est au centre pour la mettre en avant et
 * synchroniser les points.
 */
export function FeatureCarousel({ slides }: { slides: FeatureSlide[] }) {
  const trackRef = useRef<HTMLDivElement>(null);
  const [active, setActive] = useState(0);
  // Carte visée par le dernier clic, avant que le défilement fluide n'y
  // arrive : deux clics rapides sur "suivant" avancent bien de deux cartes
  // au lieu de viser deux fois la même.
  const targetRef = useRef(0);

  useEffect(() => {
    const track = trackRef.current;
    if (!track) return;
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue;
          const index = Number((entry.target as HTMLElement).dataset.index);
          setActive(index);
          targetRef.current = index;
        }
      },
      // Bande verticale étroite au centre de la piste : une seule carte à la fois.
      { root: track, rootMargin: "0px -45% 0px -45%" }
    );
    track.querySelectorAll("[data-index]").forEach((el) => observer.observe(el));
    return () => observer.disconnect();
  }, []);

  function goTo(index: number) {
    const track = trackRef.current;
    const slide = track?.querySelector<HTMLElement>(`[data-index="${index}"]`);
    if (!track || !slide) return;
    targetRef.current = index;
    track.scrollTo({
      left: slide.offsetLeft - (track.clientWidth - slide.clientWidth) / 2,
      behavior: "smooth",
    });
  }

  return (
    <div className="relative">
      <div
        ref={trackRef}
        className="flex snap-x snap-mandatory gap-4 overflow-x-auto px-[calc(50%-9rem)] py-6 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
        aria-roledescription="carrousel"
      >
        {slides.map((slide, index) => {
          const distance = Math.abs(index - active);
          return (
            <article
              key={slide.id}
              data-index={index}
              aria-roledescription="diapositive"
              aria-label={`${index + 1} sur ${slides.length} : ${slide.title}`}
              onClick={() => goTo(index)}
              className={cn(
                "glass flex w-72 shrink-0 snap-center flex-col gap-4 rounded-3xl p-5 transition-[opacity,transform,box-shadow] duration-500 [transition-timing-function:var(--motion-ease)]",
                distance === 0 && "scale-100 opacity-100 shadow-[inset_0_0_0_1px_rgba(255,122,26,0.35),var(--lift)]",
                distance === 1 && "translate-y-2 scale-[0.94] cursor-pointer opacity-60",
                distance > 1 && "translate-y-3 scale-90 cursor-pointer opacity-25"
              )}
            >
              <div className="min-h-44">{slide.preview}</div>
              <div>
                <h3 className="font-heading text-lg font-bold">{slide.title}</h3>
                <p className="mt-1 text-sm text-muted-foreground">{slide.description}</p>
              </div>
            </article>
          );
        })}
      </div>

      <div className="mt-2 flex items-center justify-center gap-4">
        <button
          type="button"
          onClick={() => goTo(Math.max(targetRef.current - 1, 0))}
          disabled={active === 0}
          aria-label="Fonctionnalité précédente"
          className="glass-inset-quiet flex h-10 w-10 items-center justify-center rounded-full transition-opacity disabled:opacity-30"
        >
          <ChevronLeft className="h-5 w-5" />
        </button>
        <div className="flex items-center gap-2">
          {slides.map((slide, index) => (
            <button
              key={slide.id}
              type="button"
              onClick={() => goTo(index)}
              aria-label={`Aller à : ${slide.title}`}
              aria-current={index === active ? "true" : undefined}
              className={cn(
                "h-2 rounded-full bg-muted-foreground/40 transition-all duration-300",
                index === active ? "w-6 bg-primary" : "w-2"
              )}
            />
          ))}
        </div>
        <button
          type="button"
          onClick={() => goTo(Math.min(targetRef.current + 1, slides.length - 1))}
          disabled={active === slides.length - 1}
          aria-label="Fonctionnalité suivante"
          className="glass-inset-quiet flex h-10 w-10 items-center justify-center rounded-full transition-opacity disabled:opacity-30"
        >
          <ChevronRight className="h-5 w-5" />
        </button>
      </div>
    </div>
  );
}
