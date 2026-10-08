"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { NAV_SECTIONS, activeSection } from "@/lib/nav";

// Les cinq rubriques, et rien d'autre : le compte (profil, paramètres, aide)
// est dans le menu de l'avatar, en haut à droite. Plus de menu hamburger en
// plus de cette barre : deux navigations sur un même écran, c'était autant de
// pages qu'on ne trouvait jamais.
export function BottomNav() {
  const pathname = usePathname();
  const current = activeSection(pathname);

  return (
    // Barre flottante : inset-x-3.5 (14px de marge) + rayon 26px + verre dense.
    // bottom = safe-area + 14px, donc elle se dégage du home indicator iOS.
    // Nécessite pb-32 sur le <main> de l'AppShell. Pas une île : en clair,
    // elle suit le thème Parquet (verre crème) au lieu de rester marine,
    // un bloc sombre qui tranchait trop en bas d'une page claire.
    <nav
      aria-label="Navigation principale"
      className="glass-strong fixed inset-x-3.5 z-40 flex items-stretch justify-around gap-0.5 rounded-[26px] p-2 md:hidden"
      style={{ bottom: "calc(env(safe-area-inset-bottom, 0px) + 14px)" }}
    >
      {NAV_SECTIONS.map((item) => {
        const isActive = current?.href === item.href;
        const Icon = item.icon;
        return (
          <Link
            key={item.href}
            href={item.href}
            aria-current={isActive ? "page" : undefined}
            className={cn(
              // min-h-[52px] : cible tactile confortable (> 44px).
              "flex min-h-[52px] min-w-0 flex-1 flex-col items-center justify-center gap-0.5 rounded-[19px] text-[10px] font-semibold transition-all",
              isActive ? "nav-active" : "text-foreground/60"
            )}
          >
            <Icon className="h-[19px] w-[19px]" />
            <span className="max-w-full truncate px-0.5">{item.label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
