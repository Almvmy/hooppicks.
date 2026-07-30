"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Menu, X, LayoutDashboard, CalendarDays, Ticket, Trophy, User } from "lucide-react";
import { cn } from "@/lib/utils";

const navItems = [
  { href: "/dashboard", label: "Tableau de bord", icon: LayoutDashboard },
  { href: "/matches", label: "Matchs", icon: CalendarDays },
  { href: "/bets", label: "Mes paris", icon: Ticket },
  { href: "/leaderboard", label: "Classement", icon: Trophy },
  { href: "/profile", label: "Profil", icon: User },
];

export function MobileNav() {
  const [open, setOpen] = useState(false);
  const pathname = usePathname();

  return (
    <div className="md:hidden">
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-label="Ouvrir le menu"
        className="rounded-md p-2 text-foreground hover:bg-accent"
      >
        <Menu className="h-5 w-5" />
      </button>

      {open && (
        <div
          className="fixed inset-0 z-[60] backdrop-blur-xl"
          style={{ backgroundColor: "rgba(11, 17, 32, 0.92)" }}
        >
          <div className="flex h-16 items-center justify-between border-b border-border px-4">
            <span className="font-heading text-lg font-bold">
              Hoop<span className="text-primary">Picks</span>
            </span>
            <button
              type="button"
              onClick={() => setOpen(false)}
              aria-label="Fermer le menu"
              className="rounded-md p-2 hover:bg-accent"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
          <nav className="flex flex-col gap-1 p-4">
            {navItems.map((item) => {
              const isActive = pathname.startsWith(item.href);
              const Icon = item.icon;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={() => setOpen(false)}
                  className={cn(
                    "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium",
                    isActive
                      ? "bg-accent text-primary"
                      : "text-foreground/70 hover:bg-accent"
                  )}
                >
                  <Icon className="h-4 w-4" />
                  {item.label}
                </Link>
              );
            })}
          </nav>
        </div>
      )}
    </div>
  );
}