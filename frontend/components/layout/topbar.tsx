"use client";

import { LogOut, Settings } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
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
import { PlayerAvatar } from "@/components/player-avatar";
import { LogoSymbol } from "@/app/LogoSymbol";
import { logoutUser, fetchProfile } from "@/lib/api/auth";
import { NAV_ITEMS } from "@/lib/nav";

export function Topbar() {
  const router = useRouter();
  const pathname = usePathname();
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const pageTitle = NAV_ITEMS.find((item) => pathname.startsWith(item.href))?.label;

  async function handleLogout() {
    await logoutUser();
    router.push("/login");
  }

  return (
    // "border-b border-border bg-background/80 backdrop-blur" → "glass-chrome"
    // + sticky top-0 : la barre reste au-dessus du contenu qui défile, ce qui
    //   est le seul moment où le flou se voit vraiment.
    <header className="glass-chrome sticky top-0 z-30 flex h-16 items-center gap-4 px-6">
      {/* Sur mobile la sidebar (qui porte la marque) est masquée : on
          réaffiche le logo ici pour garder la marque visible en permanence,
          comme dans la maquette. Caché en desktop où la sidebar suffit. */}
      <div className="flex items-center gap-2 md:hidden">
        <MobileNav />
        {/* Icône seule (pas de wordmark) : à 402px de large, garder le texte
            "HoopPicks" ici fait toucher le solde et la cloche. */}
        <Link href="/dashboard">
          <LogoSymbol variant="compact" className="h-6 w-6 shrink-0" />
        </Link>
      </div>

      {/* Comble l'espace mort entre la sidebar et le solde/cloche/avatar sur
          desktop — inutile sur mobile où la topbar est déjà pleine. */}
      {pageTitle && (
        <h1 className="hidden font-heading text-lg font-bold md:block">{pageTitle}</h1>
      )}

      <div className="ml-auto flex items-center gap-4">
        <WalletBalance />
        <NotificationsDropdown />

        <DropdownMenu>
          <DropdownMenuTrigger className="rounded-full" data-testid="user-menu-trigger">
            {profile ? (
              <PlayerAvatar
                number={profile.avatarNumber}
                position={profile.avatarPosition}
                colorway={profile.avatarColorway}
                icon={profile.avatarIcon}
                size="sm"
              />
            ) : (
              <Avatar className="h-8 w-8">
                <AvatarFallback className="bg-secondary text-xs">…</AvatarFallback>
              </Avatar>
            )}
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-44">
            <DropdownMenuItem onClick={() => router.push("/settings")}>
              <Settings className="mr-2 h-4 w-4" />
              Paramètres
            </DropdownMenuItem>
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
