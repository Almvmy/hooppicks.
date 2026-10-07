"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { ChevronDown, Heart, Users } from "lucide-react";
import { TeamLogo } from "@/components/team-logo";
import { PlayerAvatar } from "@/components/player-avatar";
import { favoriteTeamAbbreviation, useTeamsByAbbreviation } from "@/lib/use-teams";
import { LeaderboardEntry } from "@/lib/types";
import { rankFanTeams } from "@/lib/fan-teams";
import { cn, formatSignedPoints } from "@/lib/utils";

/** Classement des communautés de supporters (cf. rankFanTeams). */
export function FanTeams({ entries, myTeam }: { entries: LeaderboardEntry[]; myTeam: string | null }) {
  const teams = useTeamsByAbbreviation();
  const [open, setOpen] = useState<string | null>(null);

  const ranking = useMemo(() => rankFanTeams(entries, teams), [entries, teams]);

  const withoutTeam = entries.filter((e) => !favoriteTeamAbbreviation(e.favoriteTeam ?? undefined, teams)).length;

  if (ranking.length === 0) {
    return (
      <div className="glass flex flex-col items-center gap-3 rounded-2xl py-10 text-center">
        <Heart className="h-8 w-8 text-muted-foreground" />
        <p className="max-w-sm text-sm text-muted-foreground">
          Aucun joueur classé n&apos;a encore choisi d&apos;équipe favorite.{" "}
          <Link href="/profile" className="font-medium text-primary hover:underline">
            Choisis la tienne
          </Link>{" "}
          pour faire gagner ta communauté.
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-muted-foreground">
        Points moyens par supporter, sur les joueurs classés cette saison.
        {withoutTeam > 0 && ` ${withoutTeam} joueur${withoutTeam > 1 ? "s" : ""} sans équipe favorite ne compte${withoutTeam > 1 ? "nt" : ""} pas.`}
      </p>
      <ol className="stagger-children flex flex-col gap-2">
        {ranking.map((team, i) => {
          const info = teams.get(team.abbreviation);
          const isOpen = open === team.abbreviation;
          const isMine = team.abbreviation === myTeam;
          const best = team.fans[0];
          return (
            <li key={team.abbreviation} className={cn("glass overflow-hidden rounded-2xl", isMine && "ring-2 ring-primary/50")}>
              <button
                type="button"
                onClick={() => setOpen(isOpen ? null : team.abbreviation)}
                aria-expanded={isOpen}
                className="flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-tint/[0.04]"
              >
                <span className={cn("w-6 shrink-0 font-mono font-bold", i === 0 ? "text-primary" : "text-muted-foreground")}>
                  {i + 1}
                </span>
                <TeamLogo abbreviation={team.abbreviation} logoUrl={info?.logoUrl} size={36} />
                <span className="min-w-0 flex-1">
                  <span className="flex items-center gap-1.5 truncate font-medium">
                    {info?.name ?? team.abbreviation}
                    {isMine && <span className="text-[11px] font-semibold text-primary">Ton équipe</span>}
                  </span>
                  <span className="flex items-center gap-1 text-xs text-muted-foreground">
                    <Users className="h-3 w-3" />
                    {team.fans.length} supporter{team.fans.length > 1 ? "s" : ""} · meilleur : {best.username}
                  </span>
                </span>
                <span className="shrink-0 text-right">
                  <span className="block font-mono font-bold">{formatSignedPoints(team.averagePoints, false)}</span>
                  <span className="block text-[10px] uppercase text-muted-foreground">pts / fan</span>
                </span>
                <ChevronDown className={cn("h-4 w-4 shrink-0 text-muted-foreground transition-transform", isOpen && "rotate-180")} />
              </button>
              {isOpen && (
                <div className="flex flex-col gap-1 px-4 pb-3 shadow-[inset_0_1px_0_var(--hairline)]">
                  <p className="pt-2 text-xs text-muted-foreground">Réussite moyenne : {team.averageWinRate}%</p>
                  {team.fans.map((fan) => (
                    <Link
                      key={fan.username}
                      href={`/u/${encodeURIComponent(fan.username)}`}
                      className="flex items-center gap-2 rounded-lg px-1 py-1 text-sm transition-colors hover:bg-tint/5"
                    >
                      <span className="w-9 shrink-0 font-mono text-xs text-muted-foreground">#{fan.rank}</span>
                      <PlayerAvatar
                        number={fan.avatarNumber}
                        position={fan.avatarPosition}
                        colorway={fan.avatarColorway}
                        icon={fan.avatarIcon}
                        size="xs"
                      />
                      <span className="min-w-0 flex-1 truncate">{fan.username}</span>
                      <span className="font-mono text-xs font-bold">{formatSignedPoints(fan.points, false)}</span>
                    </Link>
                  ))}
                </div>
              )}
            </li>
          );
        })}
      </ol>
    </div>
  );
}
