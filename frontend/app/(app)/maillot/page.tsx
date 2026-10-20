"use client";

import { use } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { AvatarEditor } from "@/components/avatar-editor";
import { PageHeader } from "@/components/page-header";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import { fetchProfile } from "@/lib/api/auth";
import { safeRedirectTarget } from "@/lib/safe-redirect";

/**
 * Le maillot du joueur : juste après l'inscription (?bienvenue=1, avec
 * « Plus tard »), depuis le menu de l'avatar, son propre maillot dans le
 * classement, ou avant de partager une carte avec le maillot par défaut.
 * `suite` : où aller une fois enregistré.
 */
export default function JerseyPage({ searchParams }: { searchParams: Promise<{ bienvenue?: string; suite?: string }> }) {
  const { bienvenue, suite } = use(searchParams);
  const welcome = bienvenue === "1";
  const next = suite ? safeRedirectTarget(suite) : null;
  const router = useRouter();
  const { data: profile, isLoading, isError } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });

  return (
    <div className="mx-auto flex w-full max-w-xl flex-col gap-6">
      <PageHeader
        badge={welcome ? "Bienvenue" : "Mon maillot"}
        title={welcome ? "Choisis ton maillot" : "Ton maillot"}
        subtitle="Il te suit partout : classement, ligues, cartes à partager."
      />

      {isLoading && <BasketballLoader label="Chargement..." />}
      {isError && <p className="text-destructive">Impossible de charger ton maillot.</p>}
      {profile && <AvatarEditor profile={profile} alwaysOpen onSaved={() => next && router.push(next)} />}

      {welcome && next && (
        <Link href={next} className="self-center text-sm text-muted-foreground hover:text-foreground hover:underline">
          Plus tard
        </Link>
      )}
    </div>
  );
}
