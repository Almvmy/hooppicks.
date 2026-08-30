"use client";

import { HelpCircle, LogOut, Settings } from "lucide-react";
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
    <header className="glass-chrome relative sticky top-0 z-30 flex h-16 items-center gap-4 px-6">
      <div className="flex items-center gap-2 md:hidden">
        <MobileNav />
      </div>

      {/* La topbar porte la marque en permanence désormais (plus dans la
          sidebar) : une seule barre continue en haut, sidebar en dessous. */}
      <Link href="/dashboard" className="flex items-center gap-2">
        <LogoSymbol variant="compact" className="h-7 w-7 shrink-0" />
        {/* Wordmark caché sous sm : à 402px de large, le garder ici fait
            toucher le solde et la cloche. */}
        <span className="hidden font-heading text-xl font-bold tracking-tight sm:inline">
          Hoop<span className="text-primary">Picks</span>
        </span>
      </Link>

      {/* Centré sur toute la largeur de la barre (pas juste dans l'espace
          restant entre logo et actions) : sinon le titre penche visuellement
          vers la gauche dès que le logo prend de la place. Caché sur mobile
          où la topbar est déjà pleine. */}
      {pageTitle && (
        <h1 className="absolute left-1/2 hidden -translate-x-1/2 font-heading text-lg font-bold md:block">
          {pageTitle}
        </h1>
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
            <DropdownMenuItem onClick={() => router.push("/about")}>
              <HelpCircle className="mr-2 h-4 w-4" />
              À propos
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
