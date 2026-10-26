"use client";

import Link from "next/link";
import { Crown, Trophy } from "lucide-react";
import { PlayerAvatar } from "@/components/player-avatar";
import { ShareImageButton } from "@/components/share-image-button";
import { ChampionPhase, PhaseChampions, WeekChampions } from "@/lib/types";
import { formatSignedPoints, monthLabel, weekLabel } from "@/lib/utils";

const GOLD = "#F5B700";

const PHASE_TITLES: Record<ChampionPhase, { one: string; many: string; of: string }> = {
  cup: { one: "Champion de la Coupe NBA", many: "Champions de la Coupe NBA", of: "de la Coupe NBA" },
  playoffs: { one: "Champion des playoffs", many: "Champions des playoffs", of: "des playoffs" },
  general: { one: "Champion HoopPicks", many: "Champions HoopPicks", of: "HoopPicks de la saison" },
  month: { one: "Champion du mois", many: "Champions du mois", of: "du mois" },
};

/** Période gagnée, sous les champions : « Octobre 2026 », « Saison 2026-27 ». */
export function championPeriodLabel(data: PhaseChampions): string {
  return data.phase === "month" ? monthLabel(data.season) : `Saison ${data.season}`;
}

/**
 * Champion(s) de la semaine passée, en tête du classement toute la semaine
 * qui suit : sans ça, le titre ne se voyait que sur le profil du gagnant.
 * Avec une phase (Coupe NBA, playoffs) : son champion, en tête de son
 * classement une fois la phase finie.
 */
/** `shareable` : faux pour une ligue, la carte à partager étant celle de tout HoopPicks. */
export function ChampionBanner({ data, shareable = true }: { data: WeekChampions | PhaseChampions; shareable?: boolean }) {
  const { champions } = data;
  if (champions.length === 0) return null;
  const solo = champions.length === 1;
  const phase = "phase" in data ? PHASE_TITLES[data.phase] : null;
  // Le trophée pour le titre de la saison, la couronne pour les autres.
  const Emblem = "phase" in data && data.phase === "general" ? Trophy : Crown;
  const heading = phase
    ? solo ? phase.one : phase.many
    : solo ? "Champion de la semaine dernière" : "Champions de la semaine dernière";
  const shareText = phase
    ? `${solo ? `@${champions[0].username} est champion` : "Les champions"} ${phase.of} ${"season" in data ? championPeriodLabel(data).toLowerCase() : ""} sur HoopPicks 👑`
    : `${solo ? `@${champions[0].username} est champion` : "Les champions"} de la semaine sur HoopPicks 👑 Qui va le détrôner ?`;

  return (
    <div
      className="relative overflow-hidden rounded-3xl p-4 sm:p-5"
      style={{
        background: `linear-gradient(120deg, color-mix(in srgb, ${GOLD} 22%, transparent), color-mix(in srgb, ${GOLD} 4%, transparent) 60%)`,
        boxShadow: `inset 0 0 0 1px color-mix(in srgb, ${GOLD} 40%, transparent)`,
      }}
    >
      <Emblem aria-hidden className="pointer-events-none absolute -right-4 -top-5 h-28 w-28 rotate-12 opacity-[0.08]" style={{ color: GOLD }} />

      <div className="relative flex items-center justify-between gap-3">
        <p className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide" style={{ color: GOLD }}>
          <Emblem className="h-3.5 w-3.5" aria-hidden />
          {heading}
        </p>
        {shareable && (
        <ShareImageButton
          src={"phase" in data ? `/champion/carte?phase=${data.phase}` : "/champion/carte"}
          fileName={"phase" in data ? `hooppicks-champion-${data.phase}.png` : "hooppicks-champion.png"}
          text={shareText}
          discreet
        />
        )}
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

      <p className="relative mt-2 text-xs text-muted-foreground">{"week" in data ? weekLabel(data.week) : championPeriodLabel(data)}</p>
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
