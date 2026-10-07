import Image from "next/image";
import Link from "next/link";
import { Radio, Trophy } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { cn, seasonLabel } from "@/lib/utils";
import { MatchDate, MatchTime, TimeZoneName } from "@/components/local-date";
import { fetchPublicLeaderboard, fetchPublicMatches, fetchPublicStats } from "@/lib/api/public";
import { Match } from "@/lib/types";
import { LogoSymbol } from "@/app/LogoSymbol";
import { TeamLogo } from "@/components/team-logo";
import { FaceOffTeams, TeamWatermarks, faceOffBackground } from "@/components/match-face-off";
import { NbaLogo } from "@/components/nba-logo";
import { LandingNav } from "@/components/landing/landing-nav";
import { KickoffCountdown } from "@/components/landing/kickoff-countdown";
import { FeatureCarousel } from "@/components/landing/feature-carousel";
import {
  BadgesPreview,
  ComboPreview,
  LeaderboardSlidePreview,
  LeaguePreview,
  MatchPreview,
  PlayerStatsPreview,
} from "@/components/landing/feature-previews";
import { CountUpOnView } from "@/components/motion/count-up-on-view";
import { InstallAppButton } from "@/components/install-app";
import { LegalFooter } from "@/components/legal-footer";

const RANK_COLORS: Record<number, string> = {
  1: "text-primary",
  2: "text-muted-foreground",
  3: "text-muted-foreground",
};

function nextScheduledMatches(matches: Match[] | undefined, limit: number): Match[] {
  if (!matches) return [];
  const now = Date.now();
  return matches
    .filter((m) => m.status === "scheduled" && new Date(m.date).getTime() >= now)
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
    .slice(0, limit);
}

function SectionTitle({ eyebrow, title }: { eyebrow: string; title: string }) {
  return (
    <div className="mb-8 text-center">
      <p className="font-mono text-xs uppercase tracking-[0.3em] text-primary">{eyebrow}</p>
      <h2 className="mt-2 font-heading text-3xl font-bold tracking-tight sm:text-4xl">{title}</h2>
    </div>
  );
}

export default async function HomePage() {
  const [leaderboard, matches, stats] = await Promise.all([
    fetchPublicLeaderboard(),
    fetchPublicMatches(),
    fetchPublicStats(),
  ]);

  const topPlayers = leaderboard?.slice(0, 5) ?? [];
  const upcoming = nextScheduledMatches(matches, 6);
  const nextMatch = upcoming[0];
  const liveCount = matches?.filter((m) => m.status === "live").length ?? 0;
  const season = seasonLabel(nextMatch ? new Date(nextMatch.date) : new Date());

  const slides = [
    {
      id: "paris",
      title: "Parie sur chaque match",
      description: "Vainqueur, écart ou total de points : choisis ton marché, la cote est figée au moment où tu valides.",
      preview: <MatchPreview match={nextMatch} />,
    },
    {
      id: "combines",
      title: "Monte des combinés",
      description: "Enchaîne plusieurs matchs dans un même ticket : les cotes se multiplient.",
      preview: <ComboPreview />,
    },
    {
      id: "classement",
      title: "Grimpe au classement",
      description: "Chaque point gagné compte dans le classement général, mis à jour après chaque match.",
      preview: <LeaderboardSlidePreview entries={topPlayers} />,
    },
    {
      id: "ligues",
      title: "Défie tes amis",
      description: "Crée une ligue privée, partage son code à 6 caractères et suivez votre propre classement.",
      preview: <LeaguePreview />,
    },
    {
      id: "badges",
      title: "Débloque des badges",
      description: "Séries, gros coups, combinés : tes exploits laissent une trace sur ton profil.",
      preview: <BadgesPreview />,
    },
    {
      id: "stats",
      title: "Analyse avant de parier",
      description: "Effectifs à jour, joueurs blessés, moyennes de la saison et feuille de match détaillée.",
      preview: <PlayerStatsPreview />,
    },
  ];

  const statItems = stats
    ? [
        { label: "Joueurs inscrits", value: stats.players },
        { label: "Tickets joués", value: stats.bets },
        { label: "Ligues privées", value: stats.leagues },
        { label: "Matchs disputés", value: stats.matchesFinished },
      ]
    : [];

  return (
    <div className="min-h-screen bg-background">
      <LandingNav />

      {/* ── Hero ─────────────────────────────────────────────────────── */}
      <section className="relative overflow-hidden px-4 pb-20 pt-16 sm:pt-24">
        <Image
          src="/images/auth-court-lines.jpg"
          alt=""
          fill
          preload
          sizes="100vw"
          className="object-cover opacity-60 [filter:brightness(1.6)_contrast(1.15)]"
        />
        <div className="absolute inset-0 bg-gradient-to-b from-background/40 via-background/60 to-background light:from-background/75 light:via-background/85" />

        <div className="stagger-children relative z-10 mx-auto flex max-w-2xl flex-col items-center text-center">
          <span className="mb-5 flex items-center gap-2 rounded-full border border-primary/30 bg-primary/10 py-1 pl-1.5 pr-3 font-mono text-xs text-primary">
            <NbaLogo size={18} />
            Saison NBA {season} · Points virtuels
          </span>
          <h1 className="flex items-center gap-3 font-heading text-5xl font-bold tracking-tight sm:text-6xl">
            <LogoSymbol className="h-12 w-12 shrink-0 sm:h-14 sm:w-14" />
            <span>
              Hoop<span className="text-primary">Picks</span>
            </span>
          </h1>
          <p className="mt-5 max-w-lg text-lg text-muted-foreground">
            Pronostique sur chaque match de la saison NBA, défie tes amis dans des ligues privées et grimpe au
            classement.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Link href="/register" className={cn(buttonVariants({ size: "lg" }))}>
              Créer un compte
            </Link>
            <Link href="/login" className={cn(buttonVariants({ size: "lg", variant: "outline" }))}>
              Se connecter
            </Link>
          </div>

          {nextMatch && (
            <div className="glass mt-12 w-full max-w-lg rounded-3xl p-5 sm:p-6">
              <p className="font-mono text-[11px] uppercase tracking-[0.25em] text-primary">Prochain coup d&apos;envoi</p>
              <div className="mt-3 flex flex-wrap items-center justify-center gap-x-3 gap-y-1 font-heading text-lg font-bold">
                <TeamLogo abbreviation={nextMatch.awayTeam.abbreviation} logoUrl={nextMatch.awayTeam.logoUrl} size={40} />
                <span>{nextMatch.awayTeam.name}</span>
                <span className="text-sm text-muted-foreground">@</span>
                <span>{nextMatch.homeTeam.name}</span>
                <TeamLogo abbreviation={nextMatch.homeTeam.abbreviation} logoUrl={nextMatch.homeTeam.logoUrl} size={40} />
              </div>
              <div className="mt-4">
                <KickoffCountdown target={nextMatch.date} />
              </div>
            </div>
          )}

          {liveCount > 0 && (
            <p className="mt-4 flex items-center gap-2 text-sm text-muted-foreground">
              <Radio className="h-4 w-4 animate-pulse text-red-500 light:text-red-700" />
              {liveCount} match{liveCount > 1 ? "s" : ""} en cours en ce moment
            </p>
          )}
        </div>
      </section>

      {/* ── Aperçu ───────────────────────────────────────────────────── */}
      <section id="apercu" className="scroll-mt-16 py-20">
        <div className="reveal-on-scroll">
          <SectionTitle eyebrow="L'application" title="Tout pour pronostiquer malin" />
          <FeatureCarousel slides={slides} />
        </div>
      </section>

      {/* ── Matchs ───────────────────────────────────────────────────── */}
      <section id="matchs" className="scroll-mt-16 px-4 py-20">
        <div className="mx-auto max-w-3xl">
          <SectionTitle eyebrow="Calendrier" title="Prochaines affiches" />
          <p className="-mt-5 mb-6 text-center text-xs text-muted-foreground">
            Heures pour <TimeZoneName />
          </p>
          {upcoming.length === 0 ? (
            <p className="text-center text-muted-foreground">Aucun match programmé pour le moment.</p>
          ) : (
            <div className="reveal-children grid gap-3 sm:grid-cols-2">
              {upcoming.map((match) => (
                <div
                  key={match.id}
                  style={faceOffBackground(match)}
                  className="glass relative overflow-hidden rounded-2xl px-4 pb-4 pt-3"
                >
                  <TeamWatermarks match={match} size={150} />
                  <p className="relative mb-2 text-center font-mono text-xs text-muted-foreground">
                    <MatchDate iso={match.date} /> ·{" "}
                    <span className="font-bold text-foreground">
                      <MatchTime iso={match.date} />
                    </span>
                  </p>
                  <div className="relative">
                    <FaceOffTeams match={match} logoSize={44} nameClassName="text-sm" />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* ── Classement ───────────────────────────────────────────────── */}
      <section id="classement" className="scroll-mt-16 px-4 py-20">
        <div className="mx-auto max-w-xl">
          <SectionTitle eyebrow="Classement général" title="Les meilleurs pronostiqueurs" />
          {topPlayers.length === 0 ? (
            <p className="text-center text-muted-foreground">Le classement s&apos;ouvre avec le premier ticket joué.</p>
          ) : (
            <div className="reveal-children flex flex-col gap-2">
              {topPlayers.map((entry) => (
                <div
                  key={entry.username}
                  className={cn(
                    "flex items-center justify-between rounded-2xl px-4 py-3",
                    entry.rank === 1 ? "glass-accent" : "glass"
                  )}
                >
                  <div className="flex min-w-0 items-center gap-3">
                    <span className={cn("w-8 font-mono text-sm font-bold", RANK_COLORS[entry.rank])}>
                      #{entry.rank}
                    </span>
                    {entry.rank <= 3 && <Trophy className={cn("h-4 w-4 shrink-0", RANK_COLORS[entry.rank])} />}
                    <span className="truncate font-medium">{entry.username}</span>
                  </div>
                  <div className="shrink-0 text-right">
                    <p className="font-mono text-sm font-bold">{entry.points.toLocaleString("fr-FR")} pts</p>
                    <p className="text-[11px] text-muted-foreground">{entry.winRate}% de réussite</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* ── Chiffres ─────────────────────────────────────────────────── */}
      {statItems.length > 0 && (
        <section id="chiffres" className="scroll-mt-16 px-4 py-20">
          <div className="mx-auto max-w-4xl">
            <SectionTitle eyebrow="La communauté" title="En chiffres" />
            <div className="reveal-children grid grid-cols-2 gap-4 lg:grid-cols-4">
              {statItems.map((item) => (
                <div key={item.label} className="glass flex flex-col items-center rounded-3xl px-4 py-8 text-center">
                  <span className="font-heading text-4xl font-bold text-primary sm:text-5xl">
                    <CountUpOnView value={item.value} />
                  </span>
                  <span className="mt-2 text-xs uppercase tracking-widest text-muted-foreground">{item.label}</span>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* ── Appel final ──────────────────────────────────────────────── */}
      <section className="relative overflow-hidden px-4 py-24">
        <Image src="/images/hoop-net.jpg" alt="" fill sizes="100vw" className="object-cover opacity-30" />
        <div className="absolute inset-0 bg-gradient-to-b from-background via-background/70 to-background" />
        <div className="reveal-on-scroll relative z-10 mx-auto max-w-2xl text-center">
          <h2 className="font-heading text-3xl font-bold tracking-tight sm:text-5xl">
            Ton instinct NBA mérite un classement.
            <br />
            <span className="text-primary">Mets-le à l&apos;épreuve, match après match.</span>
          </h2>
          <Link href="/register" className={cn(buttonVariants({ size: "lg" }), "mt-8")}>
            1 000 points offerts chaque semaine
          </Link>
        </div>
      </section>

      <LegalFooter>
        <InstallAppButton />
      </LegalFooter>
    </div>
  );
}
