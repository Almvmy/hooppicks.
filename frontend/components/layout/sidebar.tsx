"use client";

import { Suspense } from "react";
import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { cn } from "@/lib/utils";
import { fetchProfile } from "@/lib/api/auth";
import { ACCOUNT_ITEMS, NAV_SECTIONS, activeSection, isTabActive, type NavTab } from "@/lib/nav";

// À part, sous Suspense : useSearchParams (onglet « Équipes » = ?tab=equipes)
// ne doit pas faire basculer toute la barre en rendu client.
function SubTabs({ tabs }: { tabs: NavTab[] }) {
  const pathname = usePathname();
  const search = useSearchParams().toString();
  return (
    <div className="ml-5 mt-1 flex flex-col gap-0.5 border-l border-tint/10 pl-3">
      {tabs.map((tab) => (
        <Link
          key={tab.href}
          href={tab.href}
          className={cn(
            "rounded-md px-2 py-1.5 text-sm transition-colors",
            isTabActive(tab, pathname, search)
              ? "font-semibold text-primary"
              : "text-sidebar-foreground/60 hover:text-sidebar-foreground"
          )}
        >
          {tab.label}
        </Link>
      ))}
    </div>
  );
}

export function Sidebar() {
  const pathname = usePathname();
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const current = activeSection(pathname);
  const account = ACCOUNT_ITEMS.filter((item) => !item.adminOnly || profile?.isAdmin);

  return (
    // "border-r border-sidebar-border bg-sidebar" → "glass-chrome-y"
    // top-16/h-[calc(100vh-4rem)] : démarre sous la topbar (h-16, pleine
    // largeur, logo compris) plutôt qu'à côté d'elle depuis le haut.
    <aside className="glass-chrome-y sticky top-16 hidden h-[calc(100vh-4rem)] w-64 shrink-0 flex-col overflow-y-auto px-4 py-6 md:flex">
      <nav aria-label="Navigation principale" className="flex flex-col gap-1">
        {NAV_SECTIONS.map((item) => {
          const isActive = current?.href === item.href;
          const Icon = item.icon;
          return (
            <div key={item.href}>
              <Link
                href={item.href}
                aria-current={isActive ? "page" : undefined}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-all",
                  // Actif : l'orange en lumière (contour + glow), plus un aplat
                  // bg-sidebar-accent.
                  isActive
                    ? "glass-accent"
                    : "text-sidebar-foreground/70 hover:bg-tint/[0.06] hover:text-sidebar-foreground"
                )}
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </Link>
              {/* Sous-pages de la rubrique ouverte seulement : le menu reste
                  à cinq lignes tant qu'on n'y entre pas. */}
              {isActive && item.tabs && (
                <Suspense fallback={null}>
                  <SubTabs tabs={item.tabs} />
                </Suspense>
              )}
            </div>
          );
        })}
      </nav>

      <nav aria-label="Compte" className="mt-auto flex flex-col gap-1 border-t border-tint/10 pt-4">
        {account.map((item) => {
          const isActive = pathname.startsWith(item.href);
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              aria-current={isActive ? "page" : undefined}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-all",
                isActive
                  ? "glass-accent"
                  : "text-sidebar-foreground/60 hover:bg-tint/[0.06] hover:text-sidebar-foreground"
              )}
            >
              <Icon className="h-4 w-4" />
              {item.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
