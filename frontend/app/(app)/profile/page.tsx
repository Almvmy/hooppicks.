"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { fetchProfile } from "@/lib/api/auth";
import { fetchWallet } from "@/lib/api/wallet";
import { BadgeGrid } from "@/components/badge-grid";

export default function ProfilePage() {
  const profileQuery = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const walletQuery = useQuery({ queryKey: ["wallet"], queryFn: fetchWallet });

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-bold">Profil</h1>
        <div className="mt-1 text-muted-foreground">
          {profileQuery.isLoading ? (
            <Skeleton className="h-4 w-32" />
          ) : profileQuery.isError ? (
            "Impossible de charger le profil."
          ) : (
            `@${profileQuery.data?.username}`
          )}
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard
          label="Solde actuel"
          value={
            walletQuery.data
              ? `${walletQuery.data.balance.toLocaleString("fr-FR")} pts`
              : undefined
          }
          isLoading={walletQuery.isLoading}
        />
        <StatCard
          label="Taux de réussite"
          value={profileQuery.data ? `${profileQuery.data.winRate}%` : undefined}
          isLoading={profileQuery.isLoading}
        />
        <StatCard
          label="Paris joués"
          value={profileQuery.data ? `${profileQuery.data.totalBets}` : undefined}
          isLoading={profileQuery.isLoading}
        />
      </div>

      <Card className="border-border bg-card">
        <CardHeader>
          <CardTitle className="font-heading text-lg">Informations</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2 text-sm">
          {profileQuery.isLoading ? (
            <>
              <Skeleton className="h-4 w-48" />
              <Skeleton className="h-4 w-32" />
            </>
          ) : profileQuery.isError ? (
            <p className="text-destructive">Erreur de chargement du profil.</p>
          ) : (
            <>
              <p>
                <span className="text-muted-foreground">E-mail : </span>
                {profileQuery.data?.email}
              </p>
              <p>
                <span className="text-muted-foreground">Équipe favorite : </span>
                {profileQuery.data?.favoriteTeam}
              </p>
            </>
          )}
        </CardContent>
      </Card>

      <div>
        <h2 className="font-heading text-lg font-bold">Badges</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Débloqués en fonction de ton activité.
        </p>
        <div className="mt-3">
          <BadgeGrid />
        </div>
      </div>

      <Link
        href="/profile/history"
        className={cn(buttonVariants({ variant: "outline" }), "w-fit")}
      >
        Voir l&apos;historique des points
      </Link>
    </div>
  );
}

function StatCard({
  label,
  value,
  isLoading,
}: {
  label: string;
  value?: string;
  isLoading: boolean;
}) {
  return (
    <Card className="border-border bg-card">
      <CardContent className="pt-6">
        <p className="text-xs uppercase tracking-wide text-muted-foreground">
          {label}
        </p>
        {isLoading ? (
          <Skeleton className="mt-2 h-7 w-20" />
        ) : (
          <p className="mt-1 font-heading text-2xl font-bold">{value}</p>
        )}
      </CardContent>
    </Card>
  );
}