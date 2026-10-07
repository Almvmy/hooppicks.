import {
  LayoutDashboard,
  Medal,
  CalendarDays,
  Newspaper,
  Settings,
  Swords,
  Shield,
  ShieldCheck,
  Ticket,
  Trophy,
  User,
  Users,
  type LucideIcon,
} from "lucide-react";

export interface NavItem {
  href: string;
  label: string;
  icon: LucideIcon;
}

export const NAV_ITEMS: NavItem[] = [
  { href: "/dashboard", label: "Tableau de bord", icon: LayoutDashboard },
  { href: "/matches", label: "Matchs", icon: CalendarDays },
  { href: "/news", label: "Actualités", icon: Newspaper },
  { href: "/players", label: "Joueurs", icon: Users },
  { href: "/bets", label: "Mes paris", icon: Ticket },
  { href: "/leaderboard", label: "Classement", icon: Trophy },
  { href: "/leagues", label: "Ligues", icon: Shield },
  { href: "/duels", label: "Duels", icon: Swords },
  { href: "/saison", label: "Pronostics de saison", icon: Medal },
  { href: "/profile", label: "Profil", icon: User },
  { href: "/settings", label: "Paramètres", icon: Settings },
  { href: "/admin", label: "Admin", icon: ShieldCheck },
];
