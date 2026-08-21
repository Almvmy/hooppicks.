"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import {
  LayoutDashboard,
  CalendarDays,
  Shield,
  ShieldCheck,
  Ticket,
  Trophy,
  User,
  Users,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { LogoSymbol } from "@/app/LogoSymbol";
import { fetchProfile } from "@/lib/api/auth";

const navItems = [
  { href: "/dashboard", label: "Tableau de bord", icon: LayoutDashboard },
  { href: "/matches", label: "Matchs", icon: CalendarDays },
  { href: "/players", label: "Joueurs", icon: Users },
  { href: "/bets", label: "Mes paris", icon: Ticket },
  { href: "/leaderboard", label: "Classement", icon: Trophy },
  { href: "/leagues", label: "Ligues", icon: Shield },
  { href: "/profile", label: "Profil", icon: User },
];

export function Sidebar() {
  const pathname = usePathname();
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });

  const items = profile?.isAdmin
    ? [...navItems, { href: "/admin", label: "Admin", icon: ShieldCheck }]
    : navItems;

  return (
    <aside className="hidden md:flex h-screen w-64 shrink-0 flex-col border-r border-sidebar-border bg-sidebar px-4 py-6">
      <div className="mb-8 flex items-center gap-2 px-2">
        <LogoSymbol variant="compact" className="h-7 w-7 shrink-0" />
        <span className="font-heading text-xl font-bold tracking-tight text-sidebar-foreground">
          Hoop<span className="text-primary">Picks</span>
        </span>
      </div>

      <nav className="flex flex-1 flex-col gap-1">
        {items.map((item) => {
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