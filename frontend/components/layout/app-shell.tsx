import { Sidebar } from "@/components/layout/sidebar";
import { Topbar } from "@/components/layout/topbar";
import { BottomNav } from "@/components/layout/bottom-nav";
import { BetSlipProvider } from "@/components/bet-slip-provider";
import { BetSlipPanel } from "@/components/bet-slip-panel";
import { BadgeUnlockWatcher } from "@/components/badge-unlock-watcher";
import { BetResultWatcher } from "@/components/bet-result-watcher";

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <BetSlipProvider>
      <div className="flex min-h-screen bg-background">
        <Sidebar />
        <div className="flex flex-1 flex-col">
          <Topbar />
          <main className="flex-1 p-6 pb-20 md:pb-6">{children}</main>
        </div>
      </div>
      <BottomNav />
      <BetSlipPanel />
      <BadgeUnlockWatcher />
      <BetResultWatcher />
    </BetSlipProvider>
  );
}