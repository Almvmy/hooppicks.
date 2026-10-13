"use client";

import { use, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { CalendarDays, LayoutDashboard, Medal, Megaphone, MessageSquare, RefreshCw, ScrollText, ShieldCheck, Ticket, Users } from "lucide-react";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import { AdminOverview } from "@/components/admin/admin-overview";
import { AdminSyncPanel } from "@/components/admin/admin-sync-panel";
import { AdminMatchesPanel } from "@/components/admin/admin-matches-panel";
import { AdminPendingBetsPanel } from "@/components/admin/admin-pending-bets-panel";
import { AdminUsersPanel } from "@/components/admin/admin-users-panel";
import { AdminAnnouncements } from "@/components/admin/admin-announcements";
import { AdminFeedbackPanel } from "@/components/admin/admin-feedback-panel";
import { AdminAuditLog } from "@/components/admin/admin-audit-log";
import { AdminSeasonResults } from "@/components/admin/admin-season-results";
import { fetchProfile } from "@/lib/api/auth";
import { fetchAdminFeedback } from "@/lib/api/admin";
import { cn } from "@/lib/utils";

const TABS = [
  { value: "apercu", label: "Vue d'ensemble", icon: LayoutDashboard },
  { value: "synchros", label: "Synchros", icon: RefreshCw },
  { value: "matchs", label: "Matchs", icon: CalendarDays },
  { value: "paris", label: "Paris", icon: Ticket },
  { value: "utilisateurs", label: "Utilisateurs", icon: Users },
  { value: "messages", label: "Messages", icon: MessageSquare },
  { value: "annonces", label: "Annonces", icon: Megaphone },
  { value: "saison", label: "Saison", icon: Medal },
  { value: "journal", label: "Journal", icon: ScrollText },
] as const;

type Tab = (typeof TABS)[number]["value"];

export default function AdminPage({ searchParams }: { searchParams: Promise<{ onglet?: string }> }) {
  // Onglet dans l'adresse (?onglet=paris) : on y revient après un rechargement.
  const { onglet } = use(searchParams);
  const router = useRouter();
  const [tab, setTabState] = useState<Tab>(TABS.some((t) => t.value === onglet) ? (onglet as Tab) : "apercu");

  const profileQuery = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  // Même clé que l'onglet Messages : le compteur se met à jour dès qu'un message est traité.
  const feedbackQuery = useQuery({
    queryKey: ["admin-feedback", "OPEN"],
    queryFn: () => fetchAdminFeedback("OPEN"),
    enabled: !!profileQuery.data?.isAdmin,
  });
  const openFeedback = feedbackQuery.data?.openCount ?? 0;

  useEffect(() => {
    if (profileQuery.data && !profileQuery.data.isAdmin) {
      router.replace("/dashboard");
    }
  }, [profileQuery.data, router]);

  function setTab(next: Tab) {
    setTabState(next);
    router.replace(next === "apercu" ? "/admin" : `/admin?onglet=${next}`, { scroll: false });
  }

  if (profileQuery.isError) {
    return <p className="text-destructive">Accès impossible à vérifier. Recharger la page.</p>;
  }
  if (profileQuery.isLoading || !profileQuery.data?.isAdmin) {
    return <BasketballLoader label="Vérification des accès..." />;
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="flex items-center gap-2 font-heading text-2xl font-bold">
          <ShieldCheck className="h-6 w-6 text-primary" />
          Console admin
        </h1>
      </div>

      <div className="glass-chrome bleed py-3 md:sticky md:top-16 md:z-20">
        <div className="glass-scroll edge-scroll flex gap-2 overflow-x-auto pb-0.5" role="tablist" aria-label="Sections de la console">
          {TABS.map(({ value, label, icon: Icon }) => (
            <button
              key={value}
              type="button"
              role="tab"
              aria-selected={tab === value}
              onClick={() => setTab(value)}
              className={cn(
                "flex shrink-0 items-center gap-1.5 rounded-full px-3 py-1.5 text-sm transition-colors",
                tab === value ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
              )}
            >
              <Icon className="h-3.5 w-3.5" />
              {label}
              {value === "messages" && openFeedback > 0 && (
                <span className="rounded-full bg-primary px-1.5 text-[11px] font-bold leading-4 text-primary-foreground">{openFeedback}</span>
              )}
            </button>
          ))}
        </div>
      </div>

      {tab === "apercu" && <AdminOverview />}
      {tab === "synchros" && <AdminSyncPanel />}
      {tab === "matchs" && <AdminMatchesPanel />}
      {tab === "paris" && <AdminPendingBetsPanel />}
      {tab === "utilisateurs" && <AdminUsersPanel />}
      {tab === "messages" && <AdminFeedbackPanel />}
      {tab === "annonces" && <AdminAnnouncements />}
      {tab === "saison" && <AdminSeasonResults />}
      {tab === "journal" && <AdminAuditLog />}
    </div>
  );
}
