import { Sidebar } from "@/components/layout/sidebar";
import { Topbar } from "@/components/layout/topbar";
import { BottomNav } from "@/components/layout/bottom-nav";
import { BetSlipProvider } from "@/components/bet-slip-provider";
import { BetSlipPanel } from "@/components/bet-slip-panel";
import { BadgeUnlockWatcher } from "@/components/badge-unlock-watcher";
import { BetResultWatcher } from "@/components/bet-result-watcher";
import { EmailVerificationBanner } from "@/components/email-verification-banner";

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <BetSlipProvider>
      {/* app-field remplace bg-background : c'est le champ lumineux que le
          verre traverse. Sans lui, tous les .glass rendent gris. */}
      <div className="app-field flex min-h-screen flex-col">
        {/* Topbar en pleine largeur (logo compris) : la sidebar démarre en
            dessous plutôt qu'à côté, pour une seule barre continue en haut
            plutôt que deux panneaux avec une couture visible au logo. */}
        <Topbar />
        <div className="flex min-w-0 flex-1">
          <Sidebar />
          <div className="flex min-w-0 flex-1 flex-col">
            <EmailVerificationBanner />
            {/* pb-32 (au lieu de pb-20) : la bottom nav est devenue une île
                flottante, elle a besoin de plus de dégagement. */}
            <main className="flex-1 p-6 pb-32 md:pb-6">{children}</main>
          </div>
        </div>
      </div>
      <BottomNav />
      <BetSlipPanel />
      <BadgeUnlockWatcher />
      <BetResultWatcher />
    </BetSlipProvider>
  );
}
