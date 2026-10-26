"use client";

import Link from "next/link";
import { Crown, Trophy } from "lucide-react";
import { PlayerAvatar } from "@/components/player-avatar";
import { TeamLogo } from "@/components/team-logo";
import { CountUp } from "@/components/motion/count-up";
import { CourtWatermark } from "@/components/court-watermark";
import { RankChange, StreakBadge } from "@/components/leaderboard/trends";
import { ChampionCrown } from "@/components/leaderboard/champion-banner";
import { favoriteTeamAbbreviation, useTeamsByAbbreviation } from "@/lib/use-teams";
import { LeaderboardEntry } from "@/lib/types";
import { cn, formatSignedPoints } from "@/lib/utils";

// Or, argent, bronze : en accent (trophée, liseré, marche du podium), jamais
// en couleur de texte, pour rester lisibles dans les deux thèmes.
const MEDALS: Record<number, { color: string; label: string }> = {
  1: { color: "#F5B700", label: "Or" },
  2: { color: "#A8B3C2", label: "Argent" },
  3: { color: "#CD7F32", label: "Bronze" },
};

function PodiumSpot({
  entry,
  place,
  teams,
  isMe,
  isChampion,
}: {
  entry: LeaderboardEntry;
  place: 1 | 2 | 3;
  teams: ReturnType<typeof useTeamsByAbbreviation>;
  isMe: boolean;
  isChampion: boolean;
}) {
  // Rang réel de l'entrée (une égalité peut donner deux 2e), mais la marche
  // et la médaille suivent la place sur le podium.
  const medal = MEDALS[place];
  const favorite = favoriteTeamAbbreviation(entry.favoriteTeam ?? undefined, teams);
  const stepHeight = { 1: "h-24", 2: "h-16", 3: "h-12" }[place];

  return (
    <Link
      href={`/u/${encodeURIComponent(entry.username)}`}
      className={cn("group flex min-w-0 flex-1 flex-col items-center", place === 1 ? "order-2" : place === 2 ? "order-1" : "order-3")}
    >
      <div className="relative mb-2">
        {place === 1 && (
          <Crown className="absolute -top-6 left-1/2 h-6 w-6 -translate-x-1/2" style={{ color: medal.color }} aria-hidden />
        )}
        <div className="rounded-full p-1" style={{ boxShadow: `0 0 0 2px ${medal.color}` }}>
          <PlayerAvatar
            number={entry.avatarNumber}
            position={entry.avatarPosition}
            colorway={entry.avatarColorway}
            icon={entry.avatarIcon}
            size={place === 1 ? "lg" : "md"}
          />
        </div>
        <StreakBadge streak={entry.streak} className="absolute -bottom-1 -right-2" />
      </div>
      <span className="flex max-w-full items-center gap-1.5 font-heading text-sm font-bold group-hover:underline sm:text-base">
        <span className="truncate">{entry.username}</span>
        {isChampion && <ChampionCrown />}
        {favorite && <TeamLogo abbreviation={favorite} logoUrl={teams.get(favorite)?.logoUrl} size={18} />}
      </span>
      {isMe && <span className="text-[11px] font-semibold text-primary">C&apos;est toi</span>}
      <span className="mt-0.5 font-mono text-sm font-bold">
        <CountUp value={entry.points} format={(n) => formatSignedPoints(n)} />
      </span>
      <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
        {entry.winRate}% de réussite
        <RankChange entry={entry} />
      </span>

      <div
        className={cn("mt-3 flex w-full items-start justify-center rounded-t-2xl pt-2", stepHeight)}
        style={{ background: `linear-gradient(180deg, color-mix(in srgb, ${medal.color} 28%, transparent), color-mix(in srgb, ${medal.color} 6%, transparent))` }}
      >
        <span className="flex items-center gap-1 font-heading text-lg font-bold">
          <Trophy className="h-4 w-4" style={{ color: medal.color }} aria-label={`Médaille d'${medal.label.toLowerCase()}`} />
          {entry.rank}
        </span>
      </div>
    </Link>
  );
}

/** Place encore libre sur le podium (moins de 3 joueurs classés) : garde le 1er au centre. */
function PodiumGhost({ place }: { place: 2 | 3 }) {
  const stepHeight = { 2: "h-16", 3: "h-12" }[place];
  return (
    <div className={cn("flex min-w-0 flex-1 flex-col items-center", place === 2 ? "order-1" : "order-3")}>
      <div className="mb-2 h-[60px] w-[60px] rounded-full border-2 border-dashed border-tint/20" />
      <span className="text-sm text-muted-foreground">Place libre</span>
      <div className={cn("mt-3 flex w-full items-start justify-center rounded-t-2xl bg-tint/[0.04] pt-2", stepHeight)}>
        <span className="font-heading text-lg font-bold text-muted-foreground">{place}</span>
      </div>
    </div>
  );
}

/**
 * Les trois premiers d'un classement (tout HoopPicks ou une ligue), le 1er au
 * centre. `action` : un bouton en haut à droite (partage de l'image du podium).
 */
export function Podium({
  entries,
  username,
  champions,
  action,
}: {
  entries: LeaderboardEntry[];
  username: string | undefined;
  champions: Set<string>;
  action?: React.ReactNode;
}) {
  const teams = useTeamsByAbbreviation();
  const podium = entries.slice(0, 3);
  if (podium.length === 0) return null;
  return (
    <div className="glass relative overflow-hidden rounded-3xl px-4 pt-3 sm:px-10">
      <CourtWatermark className="pointer-events-none absolute -right-6 -top-10 h-[220px] w-[340px] opacity-[0.05]" />
      <div className={cn("relative z-10 flex justify-end sm:-mr-7", action ? "mb-6" : "mb-8")}>{action}</div>
      <div className="relative mx-auto flex max-w-xl items-end gap-3 sm:gap-6">
        {podium.map((entry, i) => (
          <PodiumSpot
            key={entry.username}
            entry={entry}
            place={(i + 1) as 1 | 2 | 3}
            teams={teams}
            isMe={entry.username === username}
            isChampion={champions.has(entry.username)}
          />
        ))}
        {podium.length < 2 && <PodiumGhost place={2} />}
        {podium.length < 3 && <PodiumGhost place={3} />}
      </div>
    </div>
  );
}
