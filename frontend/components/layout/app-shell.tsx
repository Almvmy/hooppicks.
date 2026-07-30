import { Sidebar } from "@/components/layout/sidebar";
import { Topbar } from "@/components/layout/topbar";
import { BetSlipProvider } from "@/components/bet-slip-provider";
import { BetSlipPanel } from "@/components/bet-slip-panel";

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <BetSlipProvider>
      <div className="flex min-h-screen bg-background">
        <Sidebar />
        <div className="flex flex-1 flex-col">
          <Topbar />
          <main className="flex-1 p-6">{children}</main>
        </div>
      </div>
      <BetSlipPanel />
    </BetSlipProvider>
  );
}