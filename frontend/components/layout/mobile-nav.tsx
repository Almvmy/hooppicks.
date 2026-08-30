"use client";

import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Menu, X } from "lucide-react";
import { cn } from "@/lib/utils";
import { LogoSymbol } from "@/app/LogoSymbol";
import { fetchProfile } from "@/lib/api/auth";
import { NAV_ITEMS } from "@/lib/nav";
import { BOTTOM_NAV_HREFS } from "@/components/layout/bottom-nav";

export function MobileNav() {
  const [open, setOpen] = useState(false);
  const pathname = usePathname();
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });

  // Exclut les routes déjà accessibles depuis la bottom nav (Accueil, Matchs,
  // Mes picks, Classement, Profil) : sinon le menu hamburger les affichait en
  // double, sous un libellé différent en plus (ex. "Mes paris" ici vs
  // "Mes picks" dans la bottom nav pour la même route /bets).
  const items = (profile?.isAdmin ? NAV_ITEMS : NAV_ITEMS.filter((item) => item.href !== "/admin"))
    .filter((item) => !BOTTOM_NAV_HREFS.includes(item.href));

  // Ferme le panneau dès que la route change, sans passer par un effet
  // (ajustement d'état pendant le rendu, cf. recommandation React).
  const [lastPathname, setLastPathname] = useState(pathname);
  if (pathname !== lastPathname) {
    setLastPathname(pathname);
    if (open) setOpen(false);
  }

  // Empêche la page de défiler derrière le panneau tant qu'il est ouvert.
  useEffect(() => {
    if (!open) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [open]);

  const panel = open && (
    <div
      // bg-background backdrop-blur-xl → glass-overlay : le flou est le même,
      // mais le fond est franchement opaque (94 %). Un panneau de navigation
      // translucide sur une page dense est illisible : ici le verre sert la
      // continuité visuelle, pas la transparence.
      className="glass-overlay fixed inset-0 z-[100] flex flex-col"
      role="dialog"
      aria-modal="true"
    >
      <div className="glass-hairline-b flex h-16 shrink-0 items-center justify-between px-4">
        <span className="flex items-center gap-2 font-heading text-lg font-bold">
          <LogoSymbol variant="compact" className="h-6 w-6 shrink-0" />
          <span>
            Hoop<span className="text-primary">Picks</span>
          </span>
        </span>
        <button
          type="button"
          onClick={() => setOpen(false)}
          aria-label="Fermer le menu"
          // min-h-11 min-w-11 : cible tactile 44px (elle était à 36px).
          className="flex min-h-11 min-w-11 items-center justify-center rounded-xl hover:bg-white/[0.06]"
        >
          <X className="h-5 w-5" />
        </button>
      </div>
      <nav className="flex flex-1 flex-col gap-1 p-4">
        {items.map((item) => {
          const isActive = pathname.startsWith(item.href);
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              onClick={() => setOpen(false)}
              className={cn(
                "flex min-h-11 items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium",
                isActive ? "glass-accent" : "text-foreground/70 hover:bg-white/[0.06]"
              )}
            >
              <Icon className="h-4 w-4" />
              {item.label}
            </Link>
          );
        })}
      </nav>
    </div>
  );

  return (
    <div className="md:hidden">
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-label="Ouvrir le menu"
        className="flex min-h-11 min-w-11 items-center justify-center rounded-xl text-foreground hover:bg-white/[0.06]"
      >
        <Menu className="h-5 w-5" />
      </button>

      {/*
        Portail vers document.body : le panneau doit être "fixed" par rapport à
        l'écran entier. S'il restait un descendant du <header> (qui a un
        backdrop-filter via .glass-chrome), cet ancêtre créerait un nouveau
        containing block et casserait le positionnement fixed.
        C'est exactement le même piège avec .glass qu'avec backdrop-blur : tout
        élément portant un backdrop-filter devient un containing block pour ses
        descendants fixed. Ne pas retirer ce portail.
      */}
      {panel && createPortal(panel, document.body)}
    </div>
  );
}
