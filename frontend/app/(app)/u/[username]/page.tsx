"use client";

import { use } from "react";
import { useQuery } from "@tanstack/react-query";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { PlayerCard } from "@/components/player-card";
import { fetchPublicProfile } from "@/lib/api/users";
import { badgeIcon } from "@/lib/badges";

export default function PublicProfilePage({
  params,
}: {
  params: Promise<{ username: string }>;
}) {
  const { username } = use(params);
  const { data: profile, isLoading, isError } = useQuery({
    queryKey: ["public-profile", username],
    queryFn: () => fetchPublicProfile(username),
  });

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6">
        <Skeleton className="h-56 w-40" />
        <Skeleton className="h-24 w-full" />
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

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center gap-5">
        <PlayerCard
          username={profile.username}
          number={profile.avatarNumber}
          position={profile.avatarPosition}
          colorway={profile.avatarColorway}
          icon={profile.avatarIcon}
        />
        <div>
          <h1 className="font-heading text-2xl font-bold">@{profile.username}</h1>
          {profile.favoriteTeam && (
            <p className="mt-1 text-sm text-muted-foreground">Supporter des {profile.favoriteTeam}</p>
          )}
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Card>
          <CardContent className="pt-6">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Taux de réussite</p>
            <p className="mt-1 font-mono text-2xl font-bold">{profile.winRate}%</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">Paris joués</p>
            <p className="mt-1 font-mono text-2xl font-bold">{profile.totalBets}</p>
          </CardContent>
        </Card>
      </div>

      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="font-heading text-lg font-bold">Badges</h2>
          {unlockedBadges.length > 0 && (
            <span className="text-xs font-medium text-muted-foreground">
              {unlockedBadges.length} débloqué{unlockedBadges.length > 1 ? "s" : ""}
            </span>
          )}
        </div>
        {/* Contrairement à la page Profil (BadgeGrid), on ne montre ici que
            les badges débloqués : les verrouillés servent d'objectif perso,
            pas d'intérêt à les exposer sur le profil de quelqu'un d'autre. */}
        {unlockedBadges.length === 0 ? (
          <p className="text-sm text-muted-foreground">Aucun badge débloqué pour l&apos;instant.</p>
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
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
