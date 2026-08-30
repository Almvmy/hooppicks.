"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { LayoutDashboard, CalendarDays, Ticket, Trophy, User } from "lucide-react";
import { cn } from "@/lib/utils";

// 5 items max (repère UX standard pour une bottom nav) : le reste de
// NAV_ITEMS (Actualités, Joueurs, Ligues, Paramètres, Admin) reste dans le
// menu hamburger (mobile-nav.tsx), qui exclut ces hrefs pour ne pas les
// dupliquer : voir BOTTOM_NAV_HREFS.
export const BOTTOM_NAV_HREFS = ["/dashboard", "/matches", "/bets", "/leaderboard", "/profile"];

const navItems = [
  { href: "/dashboard", label: "Accueil", icon: LayoutDashboard },
  { href: "/matches", label: "Matchs", icon: CalendarDays },
  { href: "/bets", label: "Mes picks", icon: Ticket },
  { href: "/leaderboard", label: "Classement", icon: Trophy },
  { href: "/profile", label: "Profil", icon: User },
];

export function BottomNav() {
  const pathname = usePathname();

  return (
    // Île flottante : inset-x-3.5 (14px de marge) + rayon 26px + verre dense.
    // bottom = safe-area + 14px, donc l'île se dégage du home indicator iOS.
    // Nécessite pb-32 sur le <main> de l'AppShell.
    <nav
      className="glass-strong fixed inset-x-3.5 z-40 flex items-stretch justify-around gap-0.5 rounded-[26px] p-2 md:hidden"
      style={{ bottom: "calc(env(safe-area-inset-bottom, 0px) + 14px)" }}
    >
      {navItems.map((item) => {
        const isActive = pathname.startsWith(item.href);
        const Icon = item.icon;
        return (
          <Link
            key={item.href}
            href={item.href}
            className={cn(
              // min-h-[52px] : cible tactile confortable (> 44px).
              "flex min-h-[52px] flex-1 flex-col items-center justify-center gap-0.5 rounded-[19px] text-[10px] font-semibold transition-all",
              isActive ? "glass-accent" : "text-foreground/60"
            )}
          >
            <Icon className="h-[19px] w-[19px]" />
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}
