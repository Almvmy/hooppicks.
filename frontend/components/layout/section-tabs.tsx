"use client";

import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { activeSection, isTabActive } from "@/lib/nav";
import { cn } from "@/lib/utils";

/**
 * Onglets d'une rubrique (Compétition : Classement, Ligues, Duels, Saison ;
 * NBA : Actus, Joueurs, Équipes), en haut de chacune de ses pages sur
 * téléphone. Absents des pages de détail (une ligue, une équipe) : elles ont
 * leur propre retour.
 */
export function SectionTabs() {
  const pathname = usePathname();
  const search = useSearchParams().toString();
  const section = activeSection(pathname);
  const tabs = section?.tabs;
  if (!tabs || !tabs.some((t) => t.href.split("?")[0] === pathname)) return null;

  return (
    // md:hidden : sur ordinateur, la barre latérale déplie déjà ces onglets
    // sous la rubrique ouverte.
    <nav aria-label={section.label} className="glass-scroll edge-scroll mb-5 flex gap-2 overflow-x-auto pb-0.5 md:hidden">
      {tabs.map((tab) => {
        const active = isTabActive(tab, pathname, search);
        return (
          <Link
            key={tab.href}
            href={tab.href}
            aria-current={active ? "page" : undefined}
            className={cn(
              "flex min-h-9 shrink-0 items-center rounded-full px-4 text-sm font-semibold transition-colors",
              active ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
            )}
          >
            {tab.label}
          </Link>
        );
      })}
    </nav>
  );
}
