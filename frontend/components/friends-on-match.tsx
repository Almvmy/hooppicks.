"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Users } from "lucide-react";
import { PlayerAvatar } from "@/components/player-avatar";
import { fetchProfile } from "@/lib/api/auth";
import { fetchFriendsOnMatch } from "@/lib/api/social";
import { cn } from "@/lib/utils";

const STATUS: Record<string, string> = { won: "text-success", lost: "text-destructive line-through" };

/** Qui, dans ses ligues, a joué ce match et sur quoi. Rien sans compte, ou si personne ne l'a joué. */
export function FriendsOnMatch({ matchId }: { matchId: string }) {
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile, retry: false });
  const { data } = useQuery({
    queryKey: ["friends-on-match", matchId],
    queryFn: () => fetchFriendsOnMatch(matchId),
    enabled: !!profile,
    staleTime: 60_000,
  });
  if (!data || data.length === 0) return null;

  return (
    <div className="glass flex flex-col gap-2 rounded-2xl px-4 py-3">
      <p className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
        <Users className="h-3.5 w-3.5" />
        Tes potes sur ce match
      </p>
      <div className="flex flex-col gap-1.5">
        {data.map((f) => (
          <Link key={`${f.username}-${f.label}`} href={`/u/${encodeURIComponent(f.username)}`} className="group flex items-center gap-2 text-sm">
            <PlayerAvatar number={f.avatarNumber} position={f.avatarPosition} colorway={f.avatarColorway} icon={f.avatarIcon} size="xs" />
            <span className="min-w-0 truncate font-medium group-hover:underline">{f.username}</span>
            <span className={cn("ml-auto shrink-0 font-mono text-xs font-semibold", STATUS[f.status])}>{f.label}</span>
          </Link>
        ))}
      </div>
    </div>
  );
}
