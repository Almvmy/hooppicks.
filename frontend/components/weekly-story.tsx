"use client";

import { useCallback, useMemo, useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, Crown, Play, Shield, Swords, Ticket } from "lucide-react";
import { Big, Eyebrow, Reveal, type Slide, StoryPlayer } from "@/components/story-player";
import { ShareImageButton } from "@/components/share-image-button";
import { fetchProfile } from "@/lib/api/auth";
import { fetchWeeklyRecap, type WeeklyRecap } from "@/lib/api/weekly-recap";
import { useSeasonLaunch } from "@/lib/season-launch";
import { useLocalFlag } from "@/lib/use-local-flag";
import { WEEKLY_BANKROLL, cn, formatOdds, formatSignedPoints, weekLabel } from "@/lib/utils";

const RESULT = {
  won: { label: "Victoire", className: "text-success" },
  lost: { label: "Défaite", className: "text-destructive" },
  tie: { label: "Égalité", className: "text-jersey-muted" },
} as const;

const place = (n: number) => (n === 1 ? "1er" : `${n}e`);

function headline(recap: WeeklyRecap): string {
  if (recap.champion) return "Le patron de la semaine.";
  if (recap.net <= 0) return "Pas ta semaine.";
  if (recap.rank != null && recap.rank <= 3) return "Sur le podium.";
  return "Dans le vert.";
}

function buildSlides(recap: WeeklyRecap, username: string | undefined): Slide[] {
  const slides: Slide[] = [
    {
      key: "bilan",
      tint: recap.net > 0 ? "var(--success)" : "var(--brand)",
      render: () => (
        <div className="flex min-w-0 flex-1 flex-col justify-center gap-6">
          <Reveal>
            <Eyebrow>{weekLabel(recap.week)}</Eyebrow>
          </Reveal>
          <Reveal delay={150}>
            <Big>{headline(recap)}</Big>
          </Reveal>
          <Reveal delay={350}>
            <p className="font-heading text-7xl font-black italic tabular-nums leading-none text-brand">{formatSignedPoints(recap.points)}</p>
            <p className="mt-2 text-sm text-jersey-muted">
              {recap.won} ticket{recap.won > 1 ? "s" : ""} gagné{recap.won > 1 ? "s" : ""} sur {recap.tickets}
              {recap.net < 0 && ` · bilan de ${recap.net.toLocaleString("fr-FR")} pts, compté 0 au classement`}
            </p>
          </Reveal>
        </div>
      ),
    },
  ];

  if (recap.best) {
    const best = recap.best;
    slides.push({
      key: "coup",
      tint: "var(--paint)",
      render: () => (
        <div className="flex min-w-0 flex-1 flex-col justify-center gap-6">
          <Reveal>
            <Eyebrow>Ton meilleur coup</Eyebrow>
          </Reveal>
          <Reveal delay={150}>
            <Big>{formatSignedPoints(best.profit)}</Big>
          </Reveal>
          <Reveal delay={350}>
            <p className="flex items-start gap-2 text-lg font-semibold">
              <Ticket className="mt-1 h-5 w-5 shrink-0 text-brand" />
              <span className="[overflow-wrap:anywhere]">{best.labels}</span>
            </p>
            <p className="mt-2 font-mono text-sm text-jersey-muted">Cote {formatOdds(best.odds)}</p>
          </Reveal>
        </div>
      ),
    });
  }

  if (recap.rank != null) {
    const rank = recap.rank;
    slides.push({
      key: "place",
      tint: recap.champion ? "#F5B700" : "var(--brand)",
      render: () => (
        <div className="flex min-w-0 flex-1 flex-col justify-center gap-6">
          <Reveal>
            <Eyebrow>Sur tout HoopPicks</Eyebrow>
          </Reveal>
          <Reveal delay={150}>
            <Big className="text-7xl sm:text-8xl">#{rank}</Big>
            <p className="mt-2 text-sm text-jersey-muted">sur {recap.players} joueur{recap.players > 1 ? "s" : ""} cette semaine-là</p>
          </Reveal>
          {recap.champion && (
            <Reveal delay={350}>
              <p className="flex items-center gap-2 text-lg font-bold" style={{ color: "#F5B700" }}>
                <Crown className="h-5 w-5" />
                Champion de la semaine
              </p>
            </Reveal>
          )}
        </div>
      ),
    });
  }

  if (recap.leagues.length > 0 || recap.duels.length > 0) {
    slides.push({
      key: "potes",
      tint: "var(--paint)",
      render: () => (
        <div className="flex min-w-0 flex-1 flex-col justify-center gap-6">
          <Reveal>
            <Eyebrow>Entre potes</Eyebrow>
          </Reveal>
          <Reveal delay={150} className="flex flex-col gap-3">
            {recap.leagues.slice(0, 4).map((l) => (
              <p key={l.id} className="flex items-center gap-2.5">
                {l.champion ? <Crown className="h-5 w-5 shrink-0" style={{ color: "#F5B700" }} /> : <Shield className="h-5 w-5 shrink-0 text-jersey-muted" />}
                <span className="min-w-0 flex-1 truncate font-semibold">{l.name}</span>
                <span className="shrink-0 font-mono font-bold">
                  {place(l.rank)}
                  <span className="font-normal text-jersey-muted"> / {l.members}</span>
                </span>
              </p>
            ))}
            {recap.duels.slice(0, 3).map((d) => (
              <p key={d.opponent} className="flex items-center gap-2.5">
                <Swords className="h-5 w-5 shrink-0 text-jersey-muted" />
                <span className="min-w-0 flex-1 truncate font-semibold">@{d.opponent}</span>
                <span className="shrink-0 font-mono text-sm">{d.mine} – {d.theirs}</span>
                <span className={cn("shrink-0 text-sm font-bold", RESULT[d.result].className)}>{RESULT[d.result].label}</span>
              </p>
            ))}
          </Reveal>
        </div>
      ),
    });
  }

  slides.push({
    key: "suite",
    tint: "var(--brand)",
    render: () => (
      <div className="flex min-w-0 flex-1 flex-col justify-center gap-6">
        <Reveal>
          <Eyebrow>Nouvelle semaine</Eyebrow>
        </Reveal>
        <Reveal delay={150}>
          <Big>{WEEKLY_BANKROLL.toLocaleString("fr-FR")} pts. Tout le monde à égalité.</Big>
        </Reveal>
        <Reveal delay={350} className="flex flex-col items-start gap-3">
          {username && (
            <ShareImageButton
              src={`/p/${encodeURIComponent(username)}/semaine/carte`}
              fileName={`ma-semaine-${recap.week}.png`}
              text={`Ma semaine sur HoopPicks : ${formatSignedPoints(recap.points)} 🏀 Viens me défier !`}
              label="Partager ma semaine"
              ownJersey
            />
          )}
          <Link href="/matches" className="flex items-center gap-1.5 text-sm font-semibold text-brand">
            Voir les matchs
            <ArrowRight className="h-4 w-4" />
          </Link>
        </Reveal>
      </div>
    ),
  });
  return slides;
}

// Du lundi 12h GMT au jeudi : après, on regarde déjà la semaine en cours.
function withinRecapWindow(now: number): boolean {
  const day = new Date(now).getUTCDay();
  const hour = new Date(now).getUTCHours();
  return (day === 1 && hour >= 12) || day === 2 || day === 3 || day === 4;
}

/**
 * Le récap de la semaine passée sur l'accueil : la story s'ouvre une fois par
 * appareil et par semaine, puis une ligne permet de la revoir. Rien si le
 * joueur n'a pas de ticket réglé la semaine passée. Attend que l'intro de la
 * saison ait été vue : deux écrans plein écran à la suite, c'est trop.
 */
export function WeeklyStoryEntry() {
  const [now] = useState(() => Date.now());
  const inWindow = useMemo(() => withinRecapWindow(now), [now]);
  const { data: recap } = useQuery({ queryKey: ["weekly-recap"], queryFn: fetchWeeklyRecap, enabled: inWindow, staleTime: 10 * 60_000 });
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const launch = useSeasonLaunch();
  const [introSeen] = useLocalFlag(`intro-saison-${launch?.season ?? "?"}`);
  const [seen, setSeen] = useLocalFlag(`story-semaine-${recap?.week ?? "?"}`);
  const [replay, setReplay] = useState(false);
  const close = useCallback(() => {
    setSeen(true);
    setReplay(false);
  }, [setSeen]);

  if (!inWindow || !recap) return null;
  const slides = buildSlides(recap, profile?.username);

  return (
    <>
      <button
        type="button"
        onClick={() => setReplay(true)}
        className="glass flex w-full items-center gap-3 rounded-2xl px-4 py-3 text-left transition-transform hover:-translate-y-0.5"
      >
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary/15">
          <Play className="h-4 w-4 text-primary" />
        </span>
        <span className="min-w-0 flex-1 text-sm font-semibold">Ta semaine passée en images</span>
        <span className="shrink-0 font-mono text-sm font-bold">{formatSignedPoints(recap.points)}</span>
      </button>
      <StoryPlayer
        open={replay || (introSeen && !seen)}
        onClose={close}
        slides={slides}
        label="Ta semaine passée"
        header={`HoopPicks · ${weekLabel(recap.week).toLowerCase()}`}
      />
    </>
  );
}
