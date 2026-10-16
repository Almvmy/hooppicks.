"use client";

import Link from "next/link";
import { Crown } from "lucide-react";
import { PlayerAvatar } from "@/components/player-avatar";
import { ShareImageButton } from "@/components/share-image-button";
import { WeekChampions } from "@/lib/types";
import { formatSignedPoints, weekLabel } from "@/lib/utils";

const GOLD = "#F5B700";

/**
 * Champion(s) de la semaine passée, en tête du classement toute la semaine
 * qui suit : sans ça, le titre ne se voyait que sur le profil du gagnant.
 */
export function ChampionBanner({ data }: { data: WeekChampions }) {
  const { champions } = data;
  if (champions.length === 0) return null;
  const solo = champions.length === 1;

  return (
    <div
      className="relative overflow-hidden rounded-3xl p-4 sm:p-5"
      style={{
        background: `linear-gradient(120deg, color-mix(in srgb, ${GOLD} 22%, transparent), color-mix(in srgb, ${GOLD} 4%, transparent) 60%)`,
        boxShadow: `inset 0 0 0 1px color-mix(in srgb, ${GOLD} 40%, transparent)`,
      }}
    >
      <Crown aria-hidden className="pointer-events-none absolute -right-4 -top-5 h-28 w-28 rotate-12 opacity-[0.08]" style={{ color: GOLD }} />

      <div className="relative flex items-center justify-between gap-3">
        <p className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide" style={{ color: GOLD }}>
          <Crown className="h-3.5 w-3.5" aria-hidden />
          {solo ? "Champion de la semaine dernière" : "Champions de la semaine dernière"}
        </p>
        <ShareImageButton
          src="/champion/carte"
          fileName="hooppicks-champion.png"
          text={`${solo ? `@${champions[0].username} est champion` : "Les champions"} de la semaine sur HoopPicks 👑 Qui va le détrôner ?`}
          discreet
        />
      </div>

      <div className="relative mt-2 flex flex-wrap items-center gap-x-6 gap-y-3">
        {champions.map((c) => (
          <Link key={c.username} href={`/u/${encodeURIComponent(c.username)}`} className="group flex min-w-0 items-center gap-3">
            <div className="rounded-full p-0.5" style={{ boxShadow: `0 0 0 2px ${GOLD}` }}>
              <PlayerAvatar number={c.avatarNumber} position={c.avatarPosition} colorway={c.avatarColorway} icon={c.avatarIcon} size={solo ? "md" : "sm"} />
            </div>
            <div className="min-w-0">
              <p className="truncate font-heading text-lg font-bold group-hover:underline">{c.username}</p>
              <p className="font-mono text-sm font-bold">{formatSignedPoints(c.points)}</p>
            </div>
          </Link>
        ))}
      </div>

      <p className="relative mt-2 text-xs text-muted-foreground">{weekLabel(data.week)}</p>
    </div>
  );
}

/** Petite couronne dorée à côté du pseudo d'un champion en titre (classement, ligues). */
export function ChampionCrown() {
  return (
    <span title="Champion de la semaine dernière" className="inline-flex">
      <Crown className="h-3.5 w-3.5" style={{ color: GOLD }} aria-label="Champion de la semaine dernière" />
    </span>
  );
}
