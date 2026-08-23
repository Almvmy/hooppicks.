"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { cn } from "@/lib/utils";
import { fetchProfile } from "@/lib/api/auth";
import { NAV_ITEMS } from "@/lib/nav";

export function Sidebar() {
  const pathname = usePathname();
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });

  const items = profile?.isAdmin
    ? NAV_ITEMS
    : NAV_ITEMS.filter((item) => item.href !== "/admin");

  return (
    // "border-r border-sidebar-border bg-sidebar" → "glass-chrome-y"
    // top-16/h-[calc(100vh-4rem)] : démarre sous la topbar (h-16, pleine
    // largeur, logo compris) plutôt qu'à côté d'elle depuis le haut.
    <aside className="glass-chrome-y sticky top-16 hidden h-[calc(100vh-4rem)] w-64 shrink-0 flex-col overflow-y-auto px-4 py-6 md:flex">
      <nav className="flex flex-1 flex-col gap-1">
        {items.map((item) => {
          const isActive = pathname.startsWith(item.href);
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-all",
                // Actif : l'orange en lumière (contour + glow), plus un aplat
                // bg-sidebar-accent.
                isActive
                  ? "glass-accent"
                  : "text-sidebar-foreground/70 hover:bg-white/[0.06] hover:text-sidebar-foreground"
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
