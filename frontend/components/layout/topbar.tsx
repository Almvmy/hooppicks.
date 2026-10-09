"use client";

import { Suspense } from "react";
import { HelpCircle, Info, LogOut, Lock } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { StandingBadge } from "@/components/standing-badge";
import { NotificationsDropdown } from "@/components/notifications-dropdown";
import { PlayerAvatar } from "@/components/player-avatar";
import { LogoSymbol } from "@/app/LogoSymbol";
import { logoutUser, fetchProfile } from "@/lib/api/auth";
import { ACCOUNT_ITEMS, pageTitle } from "@/lib/nav";

// Sous Suspense : useSearchParams (« Joueurs » / « Équipes » partagent /players).
function PageTitle() {
  const title = pageTitle(usePathname(), useSearchParams().toString());
  if (!title) return null;
  // Centré sur toute la largeur de la barre (pas juste dans l'espace restant
  // entre logo et actions) : sinon le titre penche visuellement vers la gauche
  // dès que le logo prend de la place. Caché sur mobile où la topbar est déjà
  // pleine.
  return (
    <h1 className="absolute left-1/2 hidden -translate-x-1/2 font-heading text-lg font-bold md:block">{title}</h1>
  );
}

export function Topbar() {
  const router = useRouter();
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const account = ACCOUNT_ITEMS.filter((item) => !item.adminOnly || profile?.isAdmin);

  async function handleLogout() {
    await logoutUser();
    router.push("/login");
  }

  return (
    // "border-b border-border bg-background/80 backdrop-blur" → "glass-chrome"
    // + sticky top-0 : la barre reste au-dessus du contenu qui défile, ce qui
    //   est le seul moment où le flou se voit vraiment.
    <header className="glass-chrome chrome-joined relative sticky top-0 z-30 flex h-16 items-center gap-4 px-[var(--gutter)]">
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

      <Suspense fallback={null}>
        <PageTitle />
      </Suspense>

      <div className="ml-auto flex items-center gap-4">
        <StandingBadge />
        {/* Masqué sous md : la barre mobile est déjà pleine, l'aide reste
            accessible depuis le menu du profil. */}
        <Link
          href="/help"
          aria-label="Aide et support"
          title="Aide et support"
          className="hidden h-9 w-9 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-tint/[0.06] hover:text-foreground md:flex"
        >
          <HelpCircle className="h-5 w-5" />
        </Link>
        <NotificationsDropdown />

        <DropdownMenu>
          <DropdownMenuTrigger className="rounded-full" data-testid="user-menu-trigger" aria-label="Mon compte">
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
          <DropdownMenuContent align="end" className="w-48">
            {account.map((item) => (
              <DropdownMenuItem key={item.href} onClick={() => router.push(item.href)}>
                <item.icon className="mr-2 h-4 w-4" />
                {item.label}
              </DropdownMenuItem>
            ))}
            <DropdownMenuItem onClick={() => router.push("/about")}>
              <Info className="mr-2 h-4 w-4" />
              À propos
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => router.push("/confidentialite")}>
              <Lock className="mr-2 h-4 w-4" />
              Confidentialité
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
