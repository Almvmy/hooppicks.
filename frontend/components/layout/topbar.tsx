"use client";

import { LogOut } from "lucide-react";
import { useRouter } from "next/navigation";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { WalletBalance } from "@/components/wallet-balance";
import { NotificationsDropdown } from "@/components/notifications-dropdown";
import { MobileNav } from "@/components/layout/mobile-nav";
import { logoutUser } from "@/lib/api/auth";

export function Topbar() {
  const router = useRouter();

  async function handleLogout() {
    await logoutUser();
    router.push("/login");
  }

  return (
    <header className="flex h-16 items-center justify-between border-b border-border bg-background/80 px-6 backdrop-blur">
      <MobileNav />

      <div className="flex items-center gap-4">
        <WalletBalance />
        <NotificationsDropdown />

        <DropdownMenu>
          <DropdownMenuTrigger className="rounded-full" data-testid="user-menu-trigger">
            <Avatar className="h-8 w-8">
              <AvatarFallback className="bg-secondary text-xs">AL</AvatarFallback>
            </Avatar>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-44">
            <DropdownMenuItem
              onClick={handleLogout}
              className="text-destructive focus:text-destructive"
            >
              <LogOut className="mr-2 h-4 w-4" />
              Se déconnecter
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}