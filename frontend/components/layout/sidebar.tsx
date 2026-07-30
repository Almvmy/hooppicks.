"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  CalendarDays,
  Ticket,
  Trophy,
  User,
} from "lucide-react";
import { cn } from "@/lib/utils";

const navItems = [
  { href: "/dashboard", label: "Tableau de bord", icon: LayoutDashboard },
  { href: "/matches", label: "Matchs", icon: CalendarDays },
  { href: "/bets", label: "Mes paris", icon: Ticket },
  { href: "/leaderboard", label: "Classement", icon: Trophy },
  { href: "/profile", label: "Profil", icon: User },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="hidden md:flex h-screen w-64 shrink-0 flex-col border-r border-sidebar-border bg-sidebar px-4 py-6">
      <div className="mb-8 px-2">
        <span className="font-heading text-xl font-bold tracking-tight text-sidebar-foreground">
          Hoop<span className="text-primary">Picks</span>
        </span>
      </div>

      <nav className="flex flex-1 flex-col gap-1">
        {navItems.map((item) => {
          const isActive = pathname.startsWith(item.href);
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                isActive
                  ? "bg-sidebar-accent text-primary"
                  : "text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-foreground"
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