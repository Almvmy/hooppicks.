import {
  CalendarDays,
  CircleHelp,
  House,
  Newspaper,
  Settings,
  ShieldCheck,
  Ticket,
  Trophy,
  User,
  type LucideIcon,
} from "lucide-react";

export interface NavTab {
  href: string;
  label: string;
  /** Titre de la barre du haut quand le libellé court ne suffit pas. */
  title?: string;
}

/**
 * Rubrique de la navigation principale. Mêmes cinq rubriques dans la barre
 * du bas (mobile) et la barre latérale (ordinateur) : l'app se lit pareil
 * partout. Les pages secondaires d'une rubrique (Ligues, Duels…) sont des
 * onglets en haut de page (SectionTabs), pas des entrées de menu : le menu
 * reste court et chaque page a une place évidente.
 */
export interface NavSection {
  href: string;
  label: string;
  icon: LucideIcon;
  /** Préfixes d'adresse qui allument la rubrique (pages de détail comprises). */
  routes: string[];
  tabs?: NavTab[];
}

export const NAV_SECTIONS: NavSection[] = [
  { href: "/dashboard", label: "Accueil", icon: House, routes: ["/dashboard"] },
  { href: "/matches", label: "Matchs", icon: CalendarDays, routes: ["/matches"] },
  { href: "/bets", label: "Mes paris", icon: Ticket, routes: ["/bets"] },
  {
    href: "/leaderboard",
    label: "Compétition",
    icon: Trophy,
    routes: ["/leaderboard", "/leagues", "/duels", "/saison"],
    tabs: [
      { href: "/leaderboard", label: "Classement" },
      { href: "/leagues", label: "Ligues" },
      { href: "/duels", label: "Duels" },
      // Pas une rubrique à part : on ne s'en sert qu'en début de saison
      // (pronostics) puis pour suivre son classement, d'où un simple onglet.
      { href: "/saison", label: "Saison", title: "Pronostics de saison" },
    ],
  },
  {
    href: "/news",
    label: "NBA",
    icon: Newspaper,
    routes: ["/news", "/players", "/teams"],
    tabs: [
      { href: "/news", label: "Actus" },
      { href: "/players", label: "Joueurs" },
      { href: "/players?tab=equipes", label: "Équipes" },
    ],
  },
];

/** Compte : menu de l'avatar (et bas de la barre latérale), hors navigation principale. */
export const ACCOUNT_ITEMS: (NavTab & { icon: LucideIcon; adminOnly?: boolean })[] = [
  { href: "/profile", label: "Mon profil", title: "Profil", icon: User },
  { href: "/settings", label: "Paramètres", icon: Settings },
  { href: "/help", label: "Aide et règles", icon: CircleHelp },
  { href: "/admin", label: "Console admin", icon: ShieldCheck, adminOnly: true },
];

function matchesRoute(pathname: string, route: string) {
  return pathname === route || pathname.startsWith(route + "/");
}

export function activeSection(pathname: string): NavSection | undefined {
  return NAV_SECTIONS.find((s) => s.routes.some((r) => matchesRoute(pathname, r)));
}

/** Onglet actif : l'adresse exacte, requête comprise (« Équipes » = /players?tab=equipes). */
export function isTabActive(tab: NavTab, pathname: string, search: string) {
  const [path, query = ""] = tab.href.split("?");
  if (pathname !== path) return false;
  const params = new URLSearchParams(search);
  const wanted = new URLSearchParams(query);
  // Onglet sans requête (« Joueurs ») : actif seulement si aucun autre onglet de la même page ne l'est.
  if (!query) return !params.get("tab");
  return [...wanted].every(([k, v]) => params.get(k) === v);
}

/** Titre de la barre du haut (ordinateur) : l'onglet si on est sur l'un d'eux, sinon la rubrique ou la page de compte. */
export function pageTitle(pathname: string, search: string): string | undefined {
  const section = activeSection(pathname);
  const tab = section?.tabs?.find((t) => isTabActive(t, pathname, search));
  if (tab) return tab.title ?? tab.label;
  if (section) return section.label;
  const account = ACCOUNT_ITEMS.find((item) => matchesRoute(pathname, item.href));
  return account && (account.title ?? account.label);
}
