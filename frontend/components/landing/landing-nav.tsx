"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { LogoSymbol } from "@/app/LogoSymbol";
import { cn } from "@/lib/utils";

export const LANDING_SECTIONS = [
  { id: "apercu", label: "Aperçu" },
  { id: "matchs", label: "Matchs" },
  { id: "classement", label: "Classement" },
  { id: "chiffres", label: "Chiffres" },
] as const;

export function LandingNav() {
  const [active, setActive] = useState<string | null>(null);

  // Scroll-spy : la section "active" est celle qui traverse une fine bande
  // au tiers haut de l'écran (rootMargin), donc une seule à la fois, sans
  // calcul de position à chaque événement de scroll.
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) setActive(entry.target.id);
        }
      },
      { rootMargin: "-35% 0px -60% 0px" }
    );
    for (const { id } of LANDING_SECTIONS) {
      const el = document.getElementById(id);
      if (el) observer.observe(el);
    }
    return () => observer.disconnect();
  }, []);

  return (
    <header className="glass-chrome sticky top-0 z-30 flex h-16 items-center gap-4 px-4 sm:px-6">
      <Link href="/" className="flex shrink-0 items-center gap-2 font-heading text-lg font-bold">
        <LogoSymbol className="h-7 w-7" />
        <span className="hidden sm:inline">
          Hoop<span className="text-primary">Picks</span>
        </span>
      </Link>

      <nav aria-label="Sections de la page" className="mx-auto hidden items-center gap-1 md:flex">
        {LANDING_SECTIONS.map(({ id, label }) => (
          <a
            key={id}
            href={`#${id}`}
            aria-current={active === id ? "true" : undefined}
            className={cn(
              "relative rounded-full px-3 py-1.5 text-sm text-muted-foreground transition-colors hover:text-foreground",
              active === id && "text-foreground"
            )}
          >
            {label}
            <span
              aria-hidden
              className={cn(
                "absolute inset-x-3 -bottom-0.5 h-0.5 origin-center scale-x-0 rounded-full bg-primary transition-transform duration-300",
                active === id && "scale-x-100"
              )}
            />
          </a>
        ))}
      </nav>

      <div className="ml-auto flex shrink-0 gap-2 md:ml-0">
        <Link href="/login" className={cn(buttonVariants({ size: "sm", variant: "ghost" }))}>
          Se connecter
        </Link>
        <Link href="/register" className={cn(buttonVariants({ size: "sm" }))}>
          Créer un compte
        </Link>
      </div>
    </header>
  );
}
