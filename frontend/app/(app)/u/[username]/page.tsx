"use client";

import { use } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { CalendarDays, Flame, Pencil, Swords, Users } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { PlayerAvatar } from "@/components/player-avatar";
import { TeamLogo } from "@/components/team-logo";
import { CourtWatermark } from "@/components/court-watermark";
import { FormStreak } from "@/components/form-streak";
import { BestBetTrophy } from "@/components/best-bet-trophy";
import { fetchPublicProfile } from "@/lib/api/users";
import { fetchMyStanding } from "@/lib/api/leaderboard";
import { challengeToDuel, fetchDuels } from "@/lib/api/duels";
import { badgeIcon } from "@/lib/badges";
import { netResult } from "@/lib/bet-legs";
import { rankTitle } from "@/lib/rank-title";
import { getTeamColor } from "@/lib/team-colors";
import { favoriteTeamAbbreviation, useTeamsByAbbreviation } from "@/lib/use-teams";
import { PlacedBet } from "@/lib/types";
import { cn, formatMonthYear, formatRelativeTime, formatSignedPoints } from "@/lib/utils";
import { ShareButton } from "@/components/share-button";
import { PalmaresCard } from "@/components/palmares-card";

function Stat({ label, value, hint }: { label: string; value: React.ReactNode; hint?: string }) {
  return (
    <Card size="sm">
      <CardContent>
        <p className="truncate text-[11px] uppercase tracking-wide text-muted-foreground">{label}</p>
        <div className="mt-0.5 whitespace-nowrap font-heading text-xl font-bold sm:text-2xl">{value}</div>
        {hint && <p className="truncate text-xs text-muted-foreground">{hint}</p>}
      </CardContent>
    </Card>
  );
}

const TICKET_STATUS: Record<PlacedBet["status"], { label: string; variant: "success" | "destructive" | "secondary" }> = {
  won: { label: "Gagné", variant: "success" },
  lost: { label: "Perdu", variant: "destructive" },
  void: { label: "Remboursé", variant: "secondary" },
  pending: { label: "En attente", variant: "secondary" },
};

function TicketRow({ bet }: { bet: PlacedBet }) {
  const net = netResult(bet);
  const config = TICKET_STATUS[bet.status];
  const single = bet.selections.length === 1;
  return (
    <div className="flex items-center gap-3 py-2.5">
      <Badge variant={config.variant} className="shrink-0">
        {config.label}
      </Badge>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium">
          {!single && <span className="text-muted-foreground">Combiné ×{bet.selections.length} · </span>}
          {bet.selections.map((s) => s.label).join(" · ")}
        </p>
        <p className="truncate text-xs text-muted-foreground">
          {[single ? bet.selections[0].matchLabel : null, bet.resolvedAt ? formatRelativeTime(bet.resolvedAt) : null]
            .filter(Boolean)
            .join(" · ")}
        </p>
      </div>
      <span
        className={cn(
          "shrink-0 font-mono text-sm font-bold",
          net > 0 ? "text-success" : net < 0 ? "text-destructive" : "text-muted-foreground"
        )}
      >
        {bet.status === "void" ? "rendu" : formatSignedPoints(net)}
      </span>
    </div>
  );
}

export default function PublicProfilePage({ params }: { params: Promise<{ username: string }> }) {
  const { username } = use(params);
  const { data: profile, isLoading, isError } = useQuery({
    queryKey: ["public-profile", username],
    queryFn: () => fetchPublicProfile(username),
  });
  // Pour le face-à-face de la semaine : inutile sur son propre profil.
  const mineQuery = useQuery({
    queryKey: ["leaderboard", "me"],
    queryFn: fetchMyStanding,
    enabled: !!profile && !profile.isMe,
  });
  const teams = useTeamsByAbbreviation();
  const queryClient = useQueryClient();
  const duelsQuery = useQuery({ queryKey: ["duels"], queryFn: fetchDuels, enabled: !!profile && !profile.isMe });
  const challenge = useMutation({
    mutationFn: () => challengeToDuel(username),
    onSuccess: () => {
      toast.success(`Défi envoyé à @${username}.`);
      queryClient.invalidateQueries({ queryKey: ["duels"] });
    },
    onError: (error) => toast.error(error instanceof Error && error.message ? error.message : "Défi impossible."),
  });

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6">
        <Skeleton className="h-40 w-full rounded-3xl" />
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-20 rounded-2xl" />
          ))}
        </div>
      </div>
    );
  }

  if (isError || !profile) {
    return (
      <div className="flex flex-col items-center gap-2 py-12 text-center">
        <p className="font-heading text-lg font-bold">Joueur introuvable</p>
        <p className="text-sm text-muted-foreground">
          @{username}
          {" "}n&apos;existe pas ou n&apos;est plus sur HoopPicks.
        </p>
      </div>
    );
  }

  const unlockedBadges = profile.badges.filter((badge) => badge.unlocked);
  const favorite = favoriteTeamAbbreviation(profile.favoriteTeam, teams);
  const favoriteTeam = favorite ? teams.get(favorite) : undefined;
  const heroColor = favorite ? getTeamColor(favorite) : "var(--brand)";
  const title = rankTitle(profile.seasonRank ?? undefined, profile.seasonPlayers);
  const mine = mineQuery.data;
  const plural = (n: number) => (n > 1 ? "s" : "");
  // Un défi en attente ou accepté porte forcément sur la semaine en cours
  // (les autres ont expiré ou sont terminés).
  const currentDuel = duelsQuery.data?.find(
    (d) => d.opponentUsername === profile.username && (d.status === "pending" || d.status === "accepted")
  );

  return (
    <div className="flex flex-col gap-6">
      {/* ── En-tête : même habillage que sa propre page Profil ─────────── */}
      <div
        className="glass relative overflow-hidden rounded-3xl p-6 sm:p-8"
        style={{
          backgroundImage: `linear-gradient(115deg, color-mix(in srgb, ${heroColor} 22%, transparent) 0%, transparent 65%), var(--glass-tint)`,
        }}
      >
        <CourtWatermark />
        {favoriteTeam && (
          <div aria-hidden className="pointer-events-none absolute -right-10 top-1/2 -translate-y-1/2 rotate-12 opacity-[0.12]">
            <TeamLogo abbreviation={favoriteTeam.abbreviation} logoUrl={favoriteTeam.logoUrl} size={240} />
          </div>
        )}
        <div className="relative flex flex-wrap items-center gap-5">
          <PlayerAvatar
            number={profile.avatarNumber}
            position={profile.avatarPosition}
            colorway={profile.avatarColorway}
            icon={profile.avatarIcon}
            size="lg"
          />
          <div className="min-w-[10rem] flex-1">
            <h1 className="truncate font-heading text-3xl font-bold">@{profile.username}</h1>
            <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1.5 text-sm text-muted-foreground">
              <span className="rounded-full border border-primary/30 bg-primary/10 px-2.5 py-0.5 font-mono text-xs font-bold text-primary">
                {title}
              </span>
              {favoriteTeam && (
                <span className="flex items-center gap-1.5">
                  <TeamLogo abbreviation={favoriteTeam.abbreviation} logoUrl={favoriteTeam.logoUrl} size={20} />
                  {profile.favoriteTeam}
                </span>
              )}
              {profile.memberSince && (
                <span className="flex items-center gap-1.5">
                  <CalendarDays className="h-3.5 w-3.5" />
                  depuis {formatMonthYear(profile.memberSince)}
                </span>
              )}
            </div>
          </div>
          {!profile.isMe && (
            <div className="w-full lg:w-auto">
              {currentDuel ? (
                <Link href="/duels">
                  <Button variant="outline" size="sm">
                    <Swords className="h-3.5 w-3.5" />
                    {currentDuel.status === "accepted"
                      ? "Duel en cours"
                      : currentDuel.iChallenged
                        ? "Défi envoyé"
                        : "Répondre à son défi"}
                  </Button>
                </Link>
              ) : (
                <Button variant="lit" size="sm" disabled={challenge.isPending || duelsQuery.isLoading} onClick={() => challenge.mutate()}>
                  <Swords className="h-3.5 w-3.5" />
                  Défier cette semaine
                </Button>
              )}
            </div>
          )}
          {profile.isMe && (
            <div className="flex w-full flex-wrap gap-2 lg:w-auto">
              <Link href="/profile">
                <Button variant="outline" size="sm">
                  <Pencil className="h-3.5 w-3.5" />
                  Modifier mon profil
                </Button>
              </Link>
              <ShareButton
                path={`/p/${encodeURIComponent(profile.username)}`}
                text="Viens me défier sur HoopPicks, les pronostics NBA entre amis 🏀"
                label="Partager mon profil"
              />
            </div>
          )}
        </div>
      </div>

      {/* ── Chiffres ───────────────────────────────────────────────────── */}
      <div className="stagger-children grid grid-cols-2 gap-2 sm:gap-3 lg:grid-cols-4">
        <Stat
          label="Points saison"
          value={formatSignedPoints(profile.seasonPoints)}
          hint={profile.seasonRank !== null ? `#${profile.seasonRank} sur ${profile.seasonPlayers}` : "pas encore classé"}
        />
        <Stat
          label="Points semaine"
          value={formatSignedPoints(profile.weekPoints)}
          hint={profile.weekRank !== null ? `#${profile.weekRank} sur ${profile.weekPlayers}` : "aucun ticket réglé"}
        />
        <Stat
          label="Réussite"
          value={profile.totalBets === 0 ? "—" : `${profile.winRate}%`}
          hint={
            profile.totalBets === 0
              ? "aucun ticket réglé"
              : `sur ${profile.totalBets} ticket${plural(profile.totalBets)} réglé${plural(profile.totalBets)}`
          }
        />
        <Stat
          label="Série en cours"
          value={
            <span className="flex items-center gap-1.5">
              {profile.currentStreak >= 3 && <Flame className="h-5 w-5 text-primary" />}
              {profile.currentStreak}
            </span>
          }
          hint={`record : ${profile.bestStreak}`}
        />
      </div>

      <PalmaresCard palmares={profile.palmares} isMe={profile.isMe} />

      {/* ── Face-à-face de la semaine et ligues en commun ──────────────── */}
      {!profile.isMe && (mine || profile.commonLeagues.length > 0) && (
        <div className="grid gap-3 sm:grid-cols-2">
          {mine && (
            <Card>
              <CardContent className="flex flex-col gap-2">
                <p className="flex items-center gap-2 text-xs uppercase tracking-wide text-muted-foreground">
                  <Swords className="h-3.5 w-3.5" />
                  Toi contre @{profile.username} cette semaine
                </p>
                <div className="flex items-baseline justify-between gap-3 font-mono">
                  <span className={cn("text-lg font-bold", mine.weekPoints >= profile.weekPoints && "text-primary")}>
                    Toi {formatSignedPoints(mine.weekPoints)}
                  </span>
                  <span className={cn("text-lg font-bold", profile.weekPoints > mine.weekPoints && "text-primary")}>
                    {formatSignedPoints(profile.weekPoints)}
                  </span>
                </div>
                <p className="text-xs text-muted-foreground">
                  {mine.weekPoints === profile.weekPoints
                    ? "À égalité pour l'instant."
                    : mine.weekPoints > profile.weekPoints
                      ? `Tu as ${(mine.weekPoints - profile.weekPoints).toLocaleString("fr-FR")} pts d'avance.`
                      : `Il te manque ${(profile.weekPoints - mine.weekPoints).toLocaleString("fr-FR")} pts pour passer devant.`}
                </p>
              </CardContent>
            </Card>
          )}
          {profile.commonLeagues.length > 0 && (
            <Card>
              <CardContent className="flex flex-col gap-2">
                <p className="flex items-center gap-2 text-xs uppercase tracking-wide text-muted-foreground">
                  <Users className="h-3.5 w-3.5" />
                  Ligue{plural(profile.commonLeagues.length)} en commun
                </p>
                <div className="flex flex-wrap gap-1.5">
                  {profile.commonLeagues.map((name) => (
                    <span key={name} className="glass-inset-quiet rounded-full px-2.5 py-1 text-sm">
                      {name}
                    </span>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      )}

      {/* ── Derniers tickets et meilleur ticket ────────────────────────── */}
      <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
        <Card>
          <CardContent className="flex flex-col gap-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="font-heading text-base font-bold">Derniers tickets</h2>
              <FormStreak bets={profile.recentTickets} />
            </div>
            {profile.recentTickets.length > 0 && (
              <div className="divide-y divide-tint/10">
                {profile.recentTickets.map((bet) => (
                  <TicketRow key={bet.id} bet={bet} />
                ))}
              </div>
            )}
            <p className="text-xs text-muted-foreground">Les tickets en attente restent privés jusqu&apos;à leur résultat.</p>
          </CardContent>
        </Card>
        {profile.bestTicket && (
          <div>
            <BestBetTrophy bets={[profile.bestTicket]} />
          </div>
        )}
      </div>

      {/* ── Badges ─────────────────────────────────────────────────────── */}
      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="font-heading text-lg font-bold">Badges</h2>
          <span className="text-xs font-medium text-muted-foreground">
            {unlockedBadges.length} sur {profile.badges.length}
          </span>
        </div>
        {/* Contrairement à la page Profil (BadgeGrid), on ne montre ici que
            les badges débloqués : les verrouillés servent d'objectif perso,
            pas d'intérêt à les exposer sur le profil de quelqu'un d'autre. */}
        {unlockedBadges.length === 0 ? (
          <p className="text-sm text-muted-foreground">Aucun badge débloqué pour l&apos;instant.</p>
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
            {unlockedBadges.map((badge) => {
              const Icon = badgeIcon(badge.icon);
              return (
                <div
                  key={badge.id}
                  className="badge-holo glass-accent relative flex flex-col items-center gap-2 overflow-hidden rounded-2xl p-4 text-center transition-transform hover:-translate-y-0.5"
                >
                  <Icon className="h-6 w-6 text-primary" />
                  <div>
                    <p className="text-sm font-medium">{badge.label}</p>
                    <p className="mt-0.5 text-xs text-muted-foreground">{badge.description}</p>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
