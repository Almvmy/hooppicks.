"use client";

import Link from "next/link";
import { ArrowRight, Flame, Sparkles } from "lucide-react";
import { TeamLogo } from "@/components/team-logo";
import { useBoost } from "@/lib/boost";
import { countdownParts, launchPhase, useNow, useSeasonLaunch } from "@/lib/season-launch";
import type { Match } from "@/lib/types";
import { bettingClosedReason, formatMatchDate, formatMatchTime, opensLater } from "@/lib/utils";

/**
 * Lancement de la saison : compte à rebours jusqu'au premier match de saison
 * régulière, puis « c'est l'ouverture » pendant la soirée, dont tous les
 * matchs ont leur cote « vainqueur » boostée. Disparaît une fois la soirée passée.
 */
export function SeasonCountdownCard({ matches }: { matches: Match[] | undefined }) {
  const launch = useSeasonLaunch();
  const boost = useBoost();
  const now = useNow();
  if (!launch) return null;
  const phase = launchPhase(launch, now);
  if (phase === "after") return null;

  const opener = matches?.find((m) => m.id === launch.openerMatchId);
  const boostedIds = new Set(boost?.openingNight ? boost.matches.map((m) => m.matchId) : []);
  const nightMatches = (matches ?? [])
    .filter((m) => boostedIds.has(m.id))
    .sort((a, b) => a.date.localeCompare(b.date));
  const { days, hours, minutes, seconds } = countdownParts(new Date(launch.openerDate).getTime() - now);
  const factor = boost ? Math.round((boost.factor - 1) * 100) : 15;
  // Le premier match tombe souvent dans la semaine de jeu suivante : paris pas encore ouverts.
  const notOpenYet = !!opener && opensLater(opener, now);

  return (
    <section className="island relative overflow-hidden rounded-2xl bg-jersey text-jersey-foreground shadow-[var(--lift-strong)]">
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0"
        style={{
          background:
            "radial-gradient(60% 80% at 100% 0%, color-mix(in srgb, var(--brand) 38%, transparent), transparent 70%), radial-gradient(50% 70% at 0% 100%, color-mix(in srgb, var(--paint) 30%, transparent), transparent 70%)",
        }}
      />
      <div aria-hidden className="pointer-events-none absolute -bottom-24 -right-24 h-72 w-72 rounded-full border-[6px] border-white/[0.06]" />
      <div aria-hidden className="pointer-events-none absolute -bottom-10 -right-10 h-40 w-40 rounded-full border-4 border-white/[0.06]" />

      <div className="relative flex flex-col gap-5 p-5 sm:p-6">
        <div className="flex items-center justify-between gap-2">
          <span className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-[0.18em] text-brand">
            <Sparkles className="h-4 w-4" />
            Saison {launch.season}
          </span>
          {opener && (
            <span className="font-mono text-xs text-jersey-muted">
              {formatMatchDate(new Date(opener.date))} · {formatMatchTime(new Date(opener.date))}
            </span>
          )}
        </div>

        {phase === "before" ? (
          <div>
            <h2 className="font-heading text-2xl font-black italic uppercase leading-none sm:text-3xl">
              La saison commence dans
            </h2>
            <div className="mt-4 grid grid-cols-4 gap-2 sm:max-w-md" role="timer" aria-live="off">
              {[
                { v: days, l: "jours" },
                { v: hours, l: "heures" },
                { v: minutes, l: "min" },
                { v: seconds, l: "s" },
              ].map(({ v, l }) => (
                <div key={l} className="rounded-xl bg-white/[0.06] px-2 py-2.5 text-center ring-1 ring-white/10">
                  <div className="font-mono text-2xl font-bold tabular-nums sm:text-3xl">{String(v).padStart(2, "0")}</div>
                  <div className="text-[10px] font-semibold uppercase tracking-wider text-jersey-muted">{l}</div>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <h2 className="font-heading text-2xl font-black italic uppercase leading-none sm:text-3xl">
            C&apos;est l&apos;ouverture <span className="text-brand">ce soir</span>
          </h2>
        )}

        {opener && phase === "before" && (
          <Link
            href={`/matches/${opener.id}`}
            className="flex items-center gap-3 rounded-xl bg-white/[0.05] px-3 py-2.5 ring-1 ring-white/10 transition-colors hover:bg-white/[0.09]"
          >
            <span className="text-[10px] font-bold uppercase tracking-wider text-jersey-muted">Premier match</span>
            <span className="flex min-w-0 flex-1 items-center justify-end gap-2 font-heading font-bold">
              <TeamLogo abbreviation={opener.awayTeam.abbreviation} logoUrl={opener.awayTeam.logoUrl} size={26} />
              <span className="truncate"><span className="sm:hidden">{opener.awayTeam.abbreviation}</span><span className="hidden sm:inline">{opener.awayTeam.name}</span></span>
              <span className="text-xs text-jersey-muted">@</span>
              <span className="truncate"><span className="sm:hidden">{opener.homeTeam.abbreviation}</span><span className="hidden sm:inline">{opener.homeTeam.name}</span></span>
              <TeamLogo abbreviation={opener.homeTeam.abbreviation} logoUrl={opener.homeTeam.logoUrl} size={26} />
            </span>
          </Link>
        )}

        {phase === "opening-night" && nightMatches.length > 0 && (
          <ul className="flex flex-col gap-1.5">
            {nightMatches.map((m) => (
              <li key={m.id}>
                <Link
                  href={`/matches/${m.id}`}
                  className="flex items-center gap-2 rounded-xl bg-white/[0.05] px-3 py-2 text-sm ring-1 ring-white/10 transition-colors hover:bg-white/[0.09]"
                >
                  <span className="w-12 shrink-0 font-mono text-xs text-jersey-muted">
                    {m.status === "scheduled" ? formatMatchTime(new Date(m.date)) : m.status === "live" ? "Direct" : "Final"}
                  </span>
                  <TeamLogo abbreviation={m.awayTeam.abbreviation} logoUrl={m.awayTeam.logoUrl} size={22} />
                  <span className="truncate font-semibold">{m.awayTeam.abbreviation}</span>
                  <span className="text-xs text-jersey-muted">@</span>
                  <span className="truncate font-semibold">{m.homeTeam.abbreviation}</span>
                  <TeamLogo abbreviation={m.homeTeam.abbreviation} logoUrl={m.homeTeam.logoUrl} size={22} />
                  <ArrowRight className="ml-auto h-4 w-4 shrink-0 text-jersey-muted" />
                </Link>
              </li>
            ))}
          </ul>
        )}

        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <p className="flex items-start gap-2 text-sm text-jersey-foreground/90">
            <Flame className="mt-0.5 h-4 w-4 shrink-0 text-brand" />
            <span>
              <strong>Soirée d&apos;ouverture</strong> : toutes les cotes « vainqueur » boostées de +{factor} %
              {boost ? ` (mise limitée à ${boost.maxStake} pts par ticket)` : ""}, et un badge à gagner.
            </span>
          </p>
          <Link
            href={opener && phase === "before" ? `/matches/${opener.id}` : "/matches"}
            className="inline-flex shrink-0 items-center justify-center gap-1.5 rounded-xl bg-brand px-4 py-2.5 text-sm font-bold text-brand-foreground transition-transform hover:-translate-y-0.5"
          >
            {notOpenYet ? "Voir le premier match" : "Parier sur l'ouverture"}
            <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
        {notOpenYet && opener && (
          <p className="-mt-2 text-xs text-jersey-muted first-letter:uppercase">{bettingClosedReason(opener, now)}.</p>
        )}
      </div>
    </section>
  );
}
