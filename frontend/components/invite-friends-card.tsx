"use client";

import { useQuery } from "@tanstack/react-query";
import { UserPlus } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { ShareButton } from "@/components/share-button";
import { fetchMyReferrals } from "@/lib/api/referrals";
import { cn } from "@/lib/utils";

/**
 * Parrainage : le lien de son profil public sert d'invitation (un ami qui
 * s'inscrit depuis cette page est crédité). 3 recrues = badge Recruteur.
 */
export function InviteFriendsCard({ username }: { username: string }) {
  const { data, isError } = useQuery({ queryKey: ["referrals", "me"], queryFn: fetchMyReferrals, staleTime: 60 * 1000 });
  const goal = data?.goal ?? 3;
  const recruits = data?.recruits ?? 0;
  const done = recruits >= goal;

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-6">
        <div className="flex items-start gap-3">
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <UserPlus className="h-4 w-4" />
          </span>
          <div className="min-w-0 flex-1">
            <p className="font-heading font-bold">Invite tes amis</p>
            <p className="text-sm text-muted-foreground">
              {done
                ? `${recruits} amis inscrits grâce à toi : badge Recruteur débloqué.`
                : `Envoie ton lien : ${goal} amis inscrits et le badge Recruteur est à toi.`}
            </p>
          </div>
        </div>
        {!isError && (
          <div className="flex items-center gap-2" aria-label={`${Math.min(recruits, goal)} sur ${goal}`}>
            {Array.from({ length: goal }, (_, i) => (
              <span key={i} className={cn("h-2 flex-1 rounded-full", i < recruits ? "bg-primary" : "bg-tint/10")} />
            ))}
            <span className="ml-1 font-mono text-xs text-muted-foreground">
              {Math.min(recruits, goal)}/{goal}
            </span>
          </div>
        )}
        <ShareButton
          path={`/p/${encodeURIComponent(username)}`}
          text="Rejoins-moi sur HoopPicks, les pronostics NBA entre amis (sans argent). La saison commence !"
          label="Envoyer mon invitation"
          className="self-start"
        />
      </CardContent>
    </Card>
  );
}
