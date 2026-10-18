"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, Flame, Medal, Sparkles, Ticket, Trophy, Users, X } from "lucide-react";
import { TeamLogo } from "@/components/team-logo";
import { ShareImageButton } from "@/components/share-image-button";
import { fetchProfile } from "@/lib/api/auth";
import { fetchBadges } from "@/lib/api/badges";
import { fetchMyStanding } from "@/lib/api/leaderboard";
import { DEFAULT_PAST_DAYS, fetchMatches } from "@/lib/api/matches";
import { fetchPublicStats } from "@/lib/api/public";
import { fetchSeasonPicks } from "@/lib/api/season-picks";
import { useBoost } from "@/lib/boost";
import { launchPhase, useNow, useSeasonLaunch } from "@/lib/season-launch";
import { formatChance, seasonChances } from "@/lib/season-odds";
import { getTeamColor } from "@/lib/team-colors";
import { favoriteTeamAbbreviation, useTeamsByAbbreviation } from "@/lib/use-teams";
import { useLocalFlag } from "@/lib/use-local-flag";
import type { Match, TeamRank } from "@/lib/types";
import { cn, formatMatchDate, formatMatchTime } from "@/lib/utils";

const SLIDE_MS = 7000;
// Une saison régulière NBA : 30 équipes × 82 matchs / 2.
const REGULAR_SEASON_GAMES = 1230;

type Slide = { key: string; tint: string; render: () => React.ReactNode };

function Eyebrow({ children }: { children: React.ReactNode }) {
  return <p className="text-xs font-bold uppercase tracking-[0.2em] text-brand">{children}</p>;
}

function Big({ children, className }: { children: React.ReactNode; className?: string }) {
  return <h2 className={cn("font-heading text-4xl font-black italic uppercase leading-[0.95] [overflow-wrap:anywhere] sm:text-5xl", className)}>{children}</h2>;
}

/** Entrée décalée des éléments d'une diapo (relancée à chaque diapo par sa clé). */
function Reveal({ delay = 0, children, className }: { delay?: number; children: React.ReactNode; className?: string }) {
  return (
    <div
      className={cn("min-w-0 animate-in fade-in slide-in-from-bottom-4 duration-700", className)}
      style={{ animationDelay: `${delay}ms`, animationFillMode: "both" }}
    >
      {children}
    </div>
  );
}

function firstRegularMatch(matches: Match[] | undefined, abbr: string | null, now: number): Match | undefined {
  if (!abbr) return undefined;
  return (matches ?? [])
    .filter((m) => m.type === "regular" && new Date(m.date).getTime() > now - 3 * 3_600_000)
    .filter((m) => m.homeTeam.abbreviation === abbr || m.awayTeam.abbreviation === abbr)
    .sort((a, b) => a.date.localeCompare(b.date))[0];
}

/**
 * Intro de la saison façon « story » : quelques écrans plein écran, une fois
 * par appareil et par saison à partir du jour de l'ouverture, puis rejouable
 * depuis la page Saison. Avance seule, touche à droite ou à gauche pour
 * naviguer, appui long pour mettre en pause.
 */
export function SeasonStory({ open, onClose }: { open: boolean; onClose: () => void }) {
  const launch = useSeasonLaunch();
  const boost = useBoost();
  const teamsMap = useTeamsByAbbreviation();
  const teams = useMemo(() => [...teamsMap.values()], [teamsMap]);
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile, enabled: open });
  const { data: picks } = useQuery({ queryKey: ["season-picks"], queryFn: fetchSeasonPicks, enabled: open });
  const { data: standing } = useQuery({ queryKey: ["leaderboard", "me"], queryFn: fetchMyStanding, enabled: open });
  const { data: badges } = useQuery({ queryKey: ["badges"], queryFn: fetchBadges, enabled: open });
  const { data: community } = useQuery({ queryKey: ["public-stats"], queryFn: fetchPublicStats, enabled: open });
  const { data: matches } = useQuery({
    queryKey: ["matches", "window", DEFAULT_PAST_DAYS],
    queryFn: () => fetchMatches(),
    enabled: open,
  });

  const [now] = useState(() => Date.now());
  const favAbbr = favoriteTeamAbbreviation(profile?.favoriteTeam, teamsMap);
  const fav: TeamRank | undefined = favAbbr ? teamsMap.get(favAbbr) : undefined;
  const champions = useMemo(() => seasonChances("champion", teams), [teams]);
  const favRank = fav ? champions.findIndex((c) => c.team.abbreviation === fav.abbreviation) : -1;
  const favMatch = firstRegularMatch(matches, favAbbr, now);
  const opener = matches?.find((m) => m.id === launch?.openerMatchId);
  const season = launch?.season ?? picks?.season ?? "";
  const picksDone = picks ? Object.keys(picks.myPicks).length : 0;
  const unlocked = badges?.filter((b) => b.unlocked).length ?? 0;
  const champPick = picks?.myPicks.champion ? teamsMap.get(picks.myPicks.champion) : undefined;
  const deadline = picks?.deadline ? new Date(picks.deadline) : null;
  const openingNightLive = launch ? launchPhase(launch, now) !== "after" : false;

  const shareParams = new URLSearchParams({ s: season, ...(picks?.myPicks ?? {}) });
  if (profile?.username) shareParams.set("u", profile.username);
  const shareSrc = `/saison/carte?${shareParams.toString()}`;

  const slides: Slide[] = [
    {
      key: "debut",
      tint: "var(--brand)",
      render: () => (
        <div className="flex min-w-0 flex-1 flex-col justify-center gap-6">
          <Reveal>
            <Eyebrow>Saison {season}</Eyebrow>
          </Reveal>
          <Reveal delay={150}>
            <Big className="text-[2.6rem] sm:text-6xl">
              La saison
              <br />
              <span className="text-brand">commence.</span>
            </Big>
          </Reveal>
          <Reveal delay={350} className="grid grid-cols-2 gap-3">
            <div className="rounded-2xl bg-white/[0.06] p-4 ring-1 ring-white/10">
              <p className="font-mono text-3xl font-bold">{REGULAR_SEASON_GAMES.toLocaleString("fr-FR")}</p>
              <p className="text-xs text-jersey-muted">matchs de saison régulière</p>
            </div>
            <div className="rounded-2xl bg-white/[0.06] p-4 ring-1 ring-white/10">
              <p className="font-mono text-3xl font-bold">30</p>
              <p className="text-xs text-jersey-muted">équipes, un seul trophée</p>
            </div>
          </Reveal>
          {opener && (
            <Reveal delay={550} className="flex items-center gap-3 rounded-2xl bg-white/[0.06] p-4 ring-1 ring-white/10">
              <TeamLogo abbreviation={opener.awayTeam.abbreviation} logoUrl={opener.awayTeam.logoUrl} size={36} />
              <div className="min-w-0 flex-1 text-center">
                <p className="text-[10px] font-bold uppercase tracking-wider text-jersey-muted">Premier match</p>
                <p className="truncate font-heading font-bold">
                  {opener.awayTeam.name} @ {opener.homeTeam.name}
                </p>
                <p className="font-mono text-xs text-jersey-muted">
                  {formatMatchDate(new Date(opener.date))} · {formatMatchTime(new Date(opener.date))}
                </p>
              </div>
              <TeamLogo abbreviation={opener.homeTeam.abbreviation} logoUrl={opener.homeTeam.logoUrl} size={36} />
            </Reveal>
          )}
        </div>
      ),
    },
    {
      key: "equipe",
      tint: fav ? getTeamColor(fav.abbreviation) : "var(--paint)",
      render: () =>
        fav ? (
          <div className="flex min-w-0 flex-1 flex-col items-center justify-center gap-5 text-center">
            <Reveal>
              <Eyebrow>Ton équipe</Eyebrow>
            </Reveal>
            <Reveal delay={150} className="animate-in zoom-in-75 duration-700">
              <div
                className="rounded-full p-6"
                style={{ background: `radial-gradient(circle, color-mix(in srgb, ${getTeamColor(fav.abbreviation)} 55%, transparent), transparent 70%)` }}
              >
                <TeamLogo abbreviation={fav.abbreviation} logoUrl={fav.logoUrl} size={150} />
              </div>
            </Reveal>
            <Reveal delay={300}>
              <Big>{fav.name}</Big>
            </Reveal>
            {favRank >= 0 && (
              <Reveal delay={450} className="rounded-2xl bg-white/[0.07] px-5 py-3 ring-1 ring-white/10">
                <p className="text-sm text-jersey-foreground/85">Chances de titre selon HoopPicks</p>
                <p className="font-mono text-3xl font-bold">
                  {formatChance(champions[favRank].pct)} <span className="text-base text-jersey-muted">· {favRank + 1}e sur 30</span>
                </p>
              </Reveal>
            )}
            {favMatch && (
              <Reveal delay={600}>
                <p className="text-sm text-jersey-foreground/85">
                  Premier match : {favMatch.homeTeam.abbreviation === fav.abbreviation ? `contre ${favMatch.awayTeam.name}` : `à ${favMatch.homeTeam.name}`},{" "}
                  {formatMatchDate(new Date(favMatch.date))} à {formatMatchTime(new Date(favMatch.date))}
                </p>
              </Reveal>
            )}
          </div>
        ) : (
          <div className="flex min-w-0 flex-1 flex-col justify-center gap-5">
            <Reveal>
              <Eyebrow>Ton équipe</Eyebrow>
            </Reveal>
            <Reveal delay={150}>
              <Big>
                Pour qui
                <br />
                tu vibres&nbsp;?
              </Big>
            </Reveal>
            <Reveal delay={300}>
              <p className="text-jersey-foreground/85">
                Choisis ton équipe favorite : son prochain match, ses blessés et ses résultats arrivent direct sur ton accueil.
              </p>
            </Reveal>
            <Reveal delay={450}>
              <Link
                href="/profile"
                onClick={onClose}
                className="inline-flex items-center gap-1.5 rounded-xl bg-brand px-4 py-2.5 text-sm font-bold text-brand-foreground"
              >
                Choisir mon équipe <ArrowRight className="h-4 w-4" />
              </Link>
            </Reveal>
          </div>
        ),
    },
    {
      key: "favoris",
      tint: "var(--brand)",
      render: () => (
        <div className="flex min-w-0 flex-1 flex-col justify-center gap-5">
          <Reveal>
            <Eyebrow>Les favoris HoopPicks</Eyebrow>
          </Reveal>
          <Reveal delay={150}>
            <Big>Qui pour le titre&nbsp;?</Big>
          </Reveal>
          <ol className="flex flex-col gap-2">
            {champions.slice(0, 5).map(({ team, pct }, i) => (
              <Reveal key={team.abbreviation} delay={300 + i * 120}>
                <li className="relative flex items-center gap-3 overflow-hidden rounded-xl bg-white/[0.05] px-3 py-2.5 ring-1 ring-white/10">
                  <span
                    aria-hidden
                    className="absolute inset-y-0 left-0"
                    style={{
                      width: `${(pct / champions[0].pct) * 100}%`,
                      background: `linear-gradient(90deg, color-mix(in srgb, ${getTeamColor(team.abbreviation)} 45%, transparent), transparent)`,
                    }}
                  />
                  <span className="relative w-5 font-mono text-sm text-jersey-muted">{i + 1}</span>
                  <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={30} />
                  <span className="relative flex-1 font-semibold">{team.name}</span>
                  <span className="relative font-mono font-bold">{formatChance(pct)}</span>
                </li>
              </Reveal>
            ))}
          </ol>
          <Reveal delay={950}>
            <p className="text-xs text-jersey-muted">Estimation d&apos;après l&apos;Elo, le classement qui fixe les cotes. Mis à jour après chaque match.</p>
          </Reveal>
        </div>
      ),
    },
    {
      key: "pronos",
      tint: champPick ? getTeamColor(champPick.abbreviation) : "var(--paint)",
      render: () => (
        <div className="flex min-w-0 flex-1 flex-col justify-center gap-5">
          <Reveal>
            <Eyebrow>Pronostics de saison</Eyebrow>
          </Reveal>
          {champPick ? (
            <>
              <Reveal delay={150}>
                <Big>
                  Ton champion&nbsp;:
                  <br />
                  <span className="text-brand">{champPick.name}</span>
                </Big>
              </Reveal>
              <Reveal delay={300} className="flex justify-center">
                <TeamLogo abbreviation={champPick.abbreviation} logoUrl={champPick.logoUrl} size={120} />
              </Reveal>
            </>
          ) : (
            <Reveal delay={150}>
              <Big>
                Qui sera
                <br />
                <span className="text-brand">champion&nbsp;?</span>
              </Big>
            </Reveal>
          )}
          <Reveal delay={450}>
            <p className="text-jersey-foreground/85">
              {picks?.locked
                ? "Les pronostics sont clos : va voir ce que pensent les autres."
                : picksDone >= (picks?.questions.length ?? 4)
                  ? "Tes 4 pronostics sont faits. Rendez-vous en juin."
                  : `${picksDone}/4 faits${deadline ? `, jusqu'au ${formatMatchDate(deadline)}` : ""} : jusqu'à 700 pts à la clé.`}
            </p>
          </Reveal>
          <Reveal delay={600}>
            <Link
              href="/saison"
              onClick={onClose}
              className="inline-flex items-center gap-1.5 rounded-xl bg-white/10 px-4 py-2.5 text-sm font-bold ring-1 ring-white/15"
            >
              <Medal className="h-4 w-4 text-brand" />
              {picks?.locked ? "Voir la révélation" : picksDone >= 4 ? "Revoir mes pronos" : "Faire mes pronos"}
            </Link>
          </Reveal>
        </div>
      ),
    },
    {
      key: "toi",
      tint: "var(--paint)",
      render: () => (
        <div className="flex min-w-0 flex-1 flex-col justify-center gap-5">
          <Reveal>
            <Eyebrow>Avant le coup d&apos;envoi</Eyebrow>
          </Reveal>
          <Reveal delay={150}>
            {profile?.username && <p className="mb-2 truncate text-lg font-bold text-brand">@{profile.username}</p>}
            <Big>
              Déjà dans
              <br />
              le match.
            </Big>
          </Reveal>
          <div className="grid grid-cols-2 gap-3">
            {[
              { icon: Ticket, value: profile?.totalBets ?? 0, label: "tickets réglés" },
              { icon: Sparkles, value: unlocked, label: `badge${unlocked > 1 ? "s" : ""} débloqué${unlocked > 1 ? "s" : ""}` },
              { icon: Trophy, value: standing?.seasonRank ? `#${standing.seasonRank}` : "—", label: "au classement" },
              { icon: Users, value: community?.players ?? "—", label: "joueurs sur HoopPicks" },
            ].map(({ icon: Icon, value, label }, i) => (
              <Reveal key={label} delay={300 + i * 120} className="rounded-2xl bg-white/[0.06] p-4 ring-1 ring-white/10">
                <Icon className="h-4 w-4 text-brand" />
                <p className="mt-2 font-mono text-3xl font-bold">{typeof value === "number" ? value.toLocaleString("fr-FR") : value}</p>
                <p className="text-xs text-jersey-muted">{label}</p>
              </Reveal>
            ))}
          </div>
        </div>
      ),
    },
    ...(openingNightLive
      ? [
          {
            key: "ouverture",
            tint: "var(--brand)",
            render: () => (
              <div className="flex min-w-0 flex-1 flex-col justify-center gap-5">
                <Reveal>
                  <Eyebrow>Soirée d&apos;ouverture</Eyebrow>
                </Reveal>
                <Reveal delay={150}>
                  <Big>
                    Toutes les cotes
                    <br />
                    <span className="text-brand">boostées.</span>
                  </Big>
                </Reveal>
                <Reveal delay={300} className="flex items-center gap-3 rounded-2xl bg-white/[0.06] p-4 ring-1 ring-white/10">
                  <Flame className="h-8 w-8 shrink-0 text-brand" />
                  <p className="text-sm text-jersey-foreground/90">
                    +{boost ? Math.round((boost.factor - 1) * 100) : 15} % sur chaque pari « vainqueur » du premier soir
                    {boost ? `, mise limitée à ${boost.maxStake} pts par ticket` : ""}.
                  </p>
                </Reveal>
                <Reveal delay={450} className="flex items-center gap-3 rounded-2xl bg-white/[0.06] p-4 ring-1 ring-white/10">
                  <Sparkles className="h-8 w-8 shrink-0 text-brand" />
                  <p className="text-sm text-jersey-foreground/90">
                    Un badge <strong>Soirée d&apos;ouverture</strong> pour ton premier ticket sur ces matchs. Il ne reviendra pas.
                  </p>
                </Reveal>
              </div>
            ),
          },
        ]
      : []),
    {
      key: "fin",
      tint: "var(--brand)",
      render: () => (
        <div className="flex min-w-0 flex-1 flex-col justify-center gap-6">
          <Reveal>
            <Big className="text-[2.6rem] sm:text-6xl">
              <span className="font-mono not-italic">1000</span> pts
              <br />
              chaque lundi.
            </Big>
          </Reveal>
          <Reveal delay={200}>
            <p className="text-lg text-jersey-foreground/90">
              Tout le monde repart à égalité à 12h GMT. Le meilleur de la semaine est sacré champion.
            </p>
          </Reveal>
          <Reveal delay={400}>
            <p className="font-heading text-3xl font-black italic uppercase text-brand">À toi de jouer.</p>
          </Reveal>
          <Reveal delay={600} className="flex flex-wrap items-center gap-2">
            <Link
              href={opener ? `/matches/${opener.id}` : "/matches"}
              onClick={onClose}
              className="inline-flex items-center gap-1.5 rounded-xl bg-brand px-5 py-3 text-sm font-bold text-brand-foreground"
            >
              Voir les matchs <ArrowRight className="h-4 w-4" />
            </Link>
            {picksDone > 0 && (
              <ShareImageButton
                src={shareSrc}
                fileName={`ma-saison-${season}.png`}
                text="Ma saison NBA commence sur HoopPicks. Et toi, qui sera champion ?"
                label="Partager mes pronos"
              />
            )}
          </Reveal>
        </div>
      ),
    },
  ];

  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const [elapsed, setElapsed] = useState(0);
  const last = slides.length - 1;
  const current = slides[Math.min(index, last)];
  const holdTimer = useRef<number | null>(null);
  const held = useRef(false);

  // Jamais onClose() dans une fonction de mise à jour d'état : elle tourne
  // pendant le rendu, et fermer met à jour le composant parent.
  const go = useCallback(
    (delta: number) => {
      setElapsed(0);
      if (index + delta > last) {
        onClose();
        return;
      }
      setIndex(Math.max(0, index + delta));
    },
    [index, last, onClose]
  );

  // Avance seule : une horloge commune, mise en pause à l'appui long.
  useEffect(() => {
    if (!open || paused) return;
    const step = 100;
    const id = window.setInterval(() => {
      setElapsed((e) => {
        if (e + step >= SLIDE_MS) {
          if (index < last) {
            setIndex((i) => i + 1);
            return 0;
          }
          return SLIDE_MS;
        }
        return e + step;
      });
    }, step);
    return () => window.clearInterval(id);
  }, [open, paused, index, last]);

  useEffect(() => {
    if (!open) return;
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
      if (e.key === "ArrowRight") go(1);
      if (e.key === "ArrowLeft") go(-1);
    }
    window.addEventListener("keydown", onKey);
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = overflow;
    };
  }, [open, go, onClose]);

  if (!open) return null;

  function onPointerDown() {
    held.current = false;
    holdTimer.current = window.setTimeout(() => {
      held.current = true;
      setPaused(true);
    }, 250);
  }

  function onPointerUp(e: React.PointerEvent<HTMLDivElement>) {
    if (holdTimer.current) window.clearTimeout(holdTimer.current);
    if (held.current) {
      setPaused(false);
      return;
    }
    // Un lien ou un bouton de la diapo garde son clic.
    if ((e.target as HTMLElement).closest("a, button")) return;
    const rect = e.currentTarget.getBoundingClientRect();
    go(e.clientX - rect.left < rect.width * 0.3 ? -1 : 1);
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={`Intro de la saison ${season}`}
      className="island fixed inset-0 z-[95] flex items-stretch justify-center bg-black/80 backdrop-blur-sm"
    >
      <div className="relative flex h-full w-full max-w-md flex-col overflow-hidden bg-jersey text-jersey-foreground sm:my-6 sm:h-auto sm:rounded-3xl sm:shadow-[var(--lift-strong)]">
        <div
          key={`bg-${current.key}`}
          aria-hidden
          className="pointer-events-none absolute inset-0 animate-in fade-in duration-700"
          style={{
            background: `radial-gradient(90% 60% at 100% 0%, color-mix(in srgb, ${current.tint} 45%, transparent), transparent 70%), radial-gradient(70% 50% at 0% 100%, color-mix(in srgb, var(--paint) 30%, transparent), transparent 70%)`,
          }}
        />
        <div aria-hidden className="pointer-events-none absolute -bottom-28 -right-28 h-80 w-80 rounded-full border-[6px] border-white/[0.06]" />

        <div className="relative z-10 flex gap-1 px-4 pt-[max(1rem,env(safe-area-inset-top))]">
          {slides.map((s, i) => (
            <div key={s.key} className="h-1 flex-1 overflow-hidden rounded-full bg-white/20">
              <div
                className="h-full rounded-full bg-white"
                style={{ width: i < index ? "100%" : i === index ? `${(elapsed / SLIDE_MS) * 100}%` : "0%" }}
              />
            </div>
          ))}
        </div>
        <div className="relative z-10 flex items-center justify-between px-4 pt-3">
          <span className="text-xs font-semibold text-jersey-muted">HoopPicks · saison {season}</span>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fermer l'intro"
            className="rounded-full p-1.5 text-jersey-foreground/80 transition-colors hover:bg-white/10 hover:text-jersey-foreground"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div
          key={current.key}
          className="relative z-0 flex min-h-[70vh] min-w-0 flex-1 select-none flex-col px-6 pb-[max(2rem,env(safe-area-inset-bottom))] pt-4 sm:min-h-[620px]"
          onPointerDown={onPointerDown}
          onPointerUp={onPointerUp}
          onPointerLeave={() => {
            if (holdTimer.current) window.clearTimeout(holdTimer.current);
            if (held.current) setPaused(false);
          }}
        >
          {current.render()}
          <p className="mt-6 text-center text-[11px] text-jersey-muted">
            {index < last ? "Touche à droite pour avancer, à gauche pour revenir" : "Touche pour fermer"}
          </p>
        </div>
      </div>
    </div>
  );
}

/**
 * Ouvre l'intro une fois par appareil et par saison, à partir du jour de
 * l'ouverture (12h GMT), sur l'accueil.
 */
export function SeasonStoryAutoOpen() {
  const launch = useSeasonLaunch();
  const now = useNow(60_000);
  const [seen, setSeen] = useLocalFlag(`intro-saison-${launch?.season ?? "?"}`);
  const due = !!launch && now >= new Date(launch.openingNightStart).getTime();
  const close = useCallback(() => setSeen(true), [setSeen]);
  return <SeasonStory open={due && !seen} onClose={close} />;
}
