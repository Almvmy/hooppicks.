"use client";

import { useCallback, useMemo, useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, Flame, Medal, Sparkles, Ticket, Trophy, Users } from "lucide-react";
import { Big, Eyebrow, Reveal, type Slide, StoryPlayer } from "@/components/story-player";
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
import { formatMatchDate, formatMatchTime } from "@/lib/utils";

// Une saison régulière NBA : 30 équipes × 82 matchs / 2.
const REGULAR_SEASON_GAMES = 1230;

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

  return <StoryPlayer open={open} onClose={onClose} slides={slides} label={`Intro de la saison ${season}`} header={`HoopPicks · saison ${season}`} />;
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
