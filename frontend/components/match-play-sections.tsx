"use client";

import { useState } from "react";
import { ChevronDown, Circle, Star } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { TeamLogo } from "@/components/team-logo";
import { getTeamColor } from "@/lib/team-colors";
import { bestRated, ratingClass } from "@/lib/player-rating";
import type { KeyPlay, LiveMatch, LiveShot } from "@/lib/live";
import type { Match, Team } from "@/lib/types";
import { cn } from "@/lib/utils";

function SectionTitle({ children }: { children: React.ReactNode }) {
  return <h2 className="font-heading text-sm font-bold uppercase tracking-wide text-muted-foreground">{children}</h2>;
}

const shortName = (name: string | null) => {
  if (!name) return "";
  const [first, ...rest] = name.split(" ");
  return rest.length ? `${first[0]}. ${rest.join(" ")}` : name;
};

/** « 21.0 » (dernière minute chez ESPN) → « 0:21 » ; « 9:29 » inchangé. */
const formatClock = (clock: string) =>
  clock.includes(":") ? clock : `0:${String(Math.floor(Number.parseFloat(clock) || 0)).padStart(2, "0")}`;

const ordinal = (n: number) => (n === 1 ? "1er" : `${n}e`);
const periodName = (p: number) => (p <= 4 ? `${ordinal(p)} quart-temps` : p === 5 ? "Prolongation" : `${p - 4}e prolongation`);

/* ---------------------------------------------------------------- Situation */

/** Possession, fautes d'équipe du quart-temps, bonus : le contexte du moment. */
export function SituationStrip({ match, live }: { match: Match; live: LiveMatch }) {
  const s = live.situation;
  if (!s) return null;
  const side = (team: Team, fouls: number | null, bonus: string | null) => (
    <span className="flex items-center gap-1.5">
      <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={16} />
      {s.possession === team.abbreviation && (
        <Circle className="h-2.5 w-2.5 fill-primary text-primary" aria-label="Possession" />
      )}
      <span className="font-mono">{fouls ?? "-"}</span>
      {bonus && <span className="rounded bg-primary/15 px-1 text-[10px] font-bold text-primary">BONUS</span>}
    </span>
  );
  return (
    <div className="glass-inset-quiet flex items-center justify-between gap-2 rounded-xl px-3 py-2 text-xs">
      {side(match.awayTeam, s.awayFouls, s.awayBonus)}
      <span className="text-center text-[11px] text-muted-foreground">
        fautes du quart-temps
        <span className="block">
          <Circle className="mr-1 inline h-2 w-2 fill-primary text-primary" />= possession
        </span>
      </span>
      {side(match.homeTeam, s.homeFouls, s.homeBonus)}
    </div>
  );
}

/* ----------------------------------------------------------- Homme du match */

export function ManOfTheMatch({ match, live }: { match: Match; live: LiveMatch }) {
  const best = bestRated(live.players);
  if (!best) return null;
  const { player, rating } = best;
  const team = player.teamAbbreviation === match.homeTeam.abbreviation ? match.homeTeam : match.awayTeam;
  const finished = live.status.state === "post";
  return (
    <Card>
      <CardContent className="flex items-center gap-3 pt-5">
        <Star className="h-5 w-5 shrink-0 fill-primary text-primary" />
        <div className="min-w-0 flex-1">
          <p className="text-[11px] uppercase tracking-wide text-muted-foreground">
            {finished ? "Homme du match" : "Meilleur joueur pour l'instant"}
          </p>
          <p className="flex items-center gap-1.5 truncate font-heading font-bold">
            <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={18} />
            {player.playerName}
          </p>
          <p className="font-mono text-[11px] text-muted-foreground">
            {player.points} pts · {player.rebounds} reb · {player.assists} pd
          </p>
        </div>
        <span className={cn("rounded-lg px-2 py-1 font-mono text-base font-bold", ratingClass(rating))}>
          {rating.toFixed(1).replace(".", ",")}
        </span>
      </CardContent>
    </Card>
  );
}

/* --------------------------------------------------------- Courbe du match */

const W = 600;
const H = 160;
const REGULATION = 4 * 720;

/**
 * Probabilité de victoire au fil du match : au-dessus de la ligne du milieu,
 * l'équipe à domicile est favorite ; en dessous, l'équipe à l'extérieur.
 */
export function WinProbabilityChart({ match, live }: { match: Match; live: LiveMatch }) {
  const points = live.winProbability;
  if (points.length < 2) return null;
  const total = Math.max(REGULATION, points[points.length - 1].elapsedSeconds);
  const x = (t: number) => (t / total) * W;
  const y = (pct: number) => (1 - pct) * H;
  const line = points.map((p, i) => `${i === 0 ? "M" : "L"}${x(p.elapsedSeconds).toFixed(1)},${y(p.homeWinPct).toFixed(1)}`).join(" ");
  const area = `${line} L${x(points[points.length - 1].elapsedSeconds).toFixed(1)},${H / 2} L${x(points[0].elapsedSeconds).toFixed(1)},${H / 2} Z`;
  const last = points[points.length - 1].homeWinPct;
  const leader = last >= 0.5 ? match.homeTeam : match.awayTeam;
  const leaderPct = Math.round((last >= 0.5 ? last : 1 - last) * 100);
  const homeColor = getTeamColor(match.homeTeam.abbreviation);
  const awayColor = getTeamColor(match.awayTeam.abbreviation);
  const markers = [720, 1440, 2160, ...Array.from({ length: Math.ceil((total - REGULATION) / 300) }, (_, i) => REGULATION + i * 300)];

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-6">
        <div className="flex items-center justify-between gap-2">
          <SectionTitle>Courbe du match</SectionTitle>
          <span className="flex items-center gap-1.5 text-xs font-semibold">
            <TeamLogo abbreviation={leader.abbreviation} logoUrl={leader.logoUrl} size={16} />
            {leaderPct} % de chances
          </span>
        </div>
        <div className="flex gap-2">
          <div className="flex flex-col justify-between py-1 text-[10px] font-bold text-muted-foreground">
            <span>{match.homeTeam.abbreviation}</span>
            <span>{match.awayTeam.abbreviation}</span>
          </div>
          <svg viewBox={`0 0 ${W} ${H}`} className="h-36 w-full" preserveAspectRatio="none" role="img"
            aria-label={`Probabilité de victoire : ${leader.name} ${leaderPct} %`}>
            <defs>
              <clipPath id="wp-top"><rect x="0" y="0" width={W} height={H / 2} /></clipPath>
              <clipPath id="wp-bottom"><rect x="0" y={H / 2} width={W} height={H / 2} /></clipPath>
            </defs>
            {markers.filter((m) => m < total).map((m) => (
              <line key={m} x1={x(m)} x2={x(m)} y1="0" y2={H} stroke="currentColor" className="text-tint/15" strokeDasharray="3 4" />
            ))}
            <line x1="0" x2={W} y1={H / 2} y2={H / 2} stroke="currentColor" className="text-tint/30" />
            <path d={area} fill={homeColor} opacity="0.35" clipPath="url(#wp-top)" />
            <path d={area} fill={awayColor} opacity="0.35" clipPath="url(#wp-bottom)" />
            <path d={line} fill="none" stroke="currentColor" className="text-foreground/80" strokeWidth="1.5" vectorEffect="non-scaling-stroke" />
          </svg>
        </div>
        {/* Libellés au milieu de chaque quart-temps (sans prolongation, sinon ils ne tombent plus juste). */}
        {total === REGULATION && (
          <div className="grid grid-cols-4 pl-8 text-center text-[10px] text-muted-foreground">
            <span>1er QT</span>
            <span>2e QT</span>
            <span>3e QT</span>
            <span>4e QT</span>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

/* ---------------------------------------------------------- Carte des tirs */

// Demi-terrain en pieds : panier en (25, 0), ligne de fond à y = −5, milieu de
// terrain à y = 42. Ligne à 3 pts : arc de 23,75 ft, coins droits à 3 ft des bords.
const CORNER_Y = Math.sqrt(23.75 ** 2 - 22 ** 2);

function Court() {
  return (
    <g fill="none" stroke="currentColor" strokeWidth="0.25" className="text-tint/40">
      <rect x="0" y="-5" width="50" height="47" />
      <rect x="17" y="-5" width="16" height="19" />
      <circle cx="25" cy="14" r="6" />
      <line x1="22" x2="28" y1="-1" y2="-1" />
      <circle cx="25" cy="0" r="0.75" />
      <path d="M21 0 A4 4 0 0 0 29 0" />
      <path d={`M3 -5 L3 ${CORNER_Y} A23.75 23.75 0 0 0 47 ${CORNER_Y} L47 -5`} />
      <path d="M19 42 A6 6 0 0 1 31 42" />
    </g>
  );
}

function ShotMark({ shot, color }: { shot: LiveShot; color: string }) {
  const cx = shot.x;
  const cy = shot.y;
  return shot.made ? (
    <circle cx={cx} cy={cy} r="0.85" fill={color} stroke="white" strokeWidth="0.15" />
  ) : (
    <g stroke={color} strokeWidth="0.3" opacity="0.7">
      <line x1={cx - 0.6} x2={cx + 0.6} y1={cy - 0.6} y2={cy + 0.6} />
      <line x1={cx - 0.6} x2={cx + 0.6} y1={cy + 0.6} y2={cy - 0.6} />
    </g>
  );
}

const pct = (made: number, total: number) => (total ? Math.round((made / total) * 100) : 0);

/** Demi-terrain avec des tirs (réussis en plein, ratés en croix) et la réussite en dessous. */
export function ShotCourt({ shots, color }: { shots: LiveShot[]; color: string }) {
  const made = shots.filter((s) => s.made).length;
  const threes = shots.filter((s) => s.points === 3);
  const threesMade = threes.filter((s) => s.made).length;
  return (
    <div className="flex flex-col gap-3">
      <svg
        viewBox="0 -5 50 47"
        className="w-full rounded-xl bg-tint/[0.04]"
        role="img"
        aria-label={`Carte des tirs : ${made} réussis sur ${shots.length}`}
      >
        <Court />
        {shots.map((s, i) => (
          <ShotMark key={i} shot={s} color={color} />
        ))}
      </svg>
      <div className="grid grid-cols-2 gap-2 text-center text-sm">
        <div className="glass-inset-quiet rounded-xl px-2 py-1.5">
          <p className="text-[11px] text-muted-foreground">Tirs</p>
          <p className="font-mono font-bold">
            {made}/{shots.length} · {pct(made, shots.length)} %
          </p>
        </div>
        <div className="glass-inset-quiet rounded-xl px-2 py-1.5">
          <p className="text-[11px] text-muted-foreground">À 3 pts</p>
          <p className="font-mono font-bold">
            {threesMade}/{threes.length} · {pct(threesMade, threes.length)} %
          </p>
        </div>
      </div>
      <p className="text-[11px] text-muted-foreground">Rond plein : réussi · croix : raté. Lancers francs non compris.</p>
    </div>
  );
}

/** Tirs d'une équipe (ou d'un de ses joueurs) sur le demi-terrain. */
export function ShotChart({ match, live }: { match: Match; live: LiveMatch }) {
  const [side, setSide] = useState<"away" | "home">("away");
  const [player, setPlayer] = useState("");
  const team = side === "home" ? match.homeTeam : match.awayTeam;
  const teamShots = live.shots.filter((s) => s.teamAbbreviation === team.abbreviation);
  // Les joueurs qui ont tiré, du plus grand nombre de tirs au plus petit, avec leur réussite.
  const shooters = [...new Set(teamShots.map((s) => s.playerName).filter((n): n is string => !!n))]
    .map((name) => {
      const own = teamShots.filter((s) => s.playerName === name);
      return { name, made: own.filter((s) => s.made).length, attempts: own.length };
    })
    .sort((a, b) => b.attempts - a.attempts || b.made - a.made);
  const shots = player ? teamShots.filter((s) => s.playerName === player) : teamShots;

  if (live.shots.length === 0) {
    return <p className="text-sm text-muted-foreground">Pas encore de tir enregistré pour ce match.</p>;
  }

  const chip = (active: boolean) =>
    cn(
      "flex shrink-0 items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold transition-colors",
      active ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
    );

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-6">
        <div className="glass-inset-quiet grid grid-cols-2 gap-1 rounded-full p-1" role="tablist" aria-label="Équipe">
          {([["away", match.awayTeam], ["home", match.homeTeam]] as const).map(([value, t]) => (
            <button
              key={value}
              type="button"
              role="tab"
              aria-selected={side === value}
              onClick={() => {
                setSide(value);
                setPlayer("");
              }}
              className={cn(
                "flex min-h-9 items-center justify-center gap-2 rounded-full text-sm font-semibold transition-colors",
                side === value ? "glass-accent" : "text-muted-foreground hover:text-foreground"
              )}
            >
              <TeamLogo abbreviation={t.abbreviation} logoUrl={t.logoUrl} size={18} />
              {t.abbreviation}
            </button>
          ))}
        </div>
        {/* Pastilles qui défilent plutôt qu'une liste déroulante : on voit d'un coup
            d'œil qui a tiré et avec quelle réussite, et on change d'un toucher. */}
        <div className="glass-scroll -mx-1 flex gap-2 overflow-x-auto px-1 pb-1" role="tablist" aria-label="Joueur">
          <button type="button" role="tab" aria-selected={!player} onClick={() => setPlayer("")} className={chip(!player)}>
            Toute l&apos;équipe
          </button>
          {shooters.map((s) => (
            <button
              key={s.name}
              type="button"
              role="tab"
              aria-selected={player === s.name}
              onClick={() => setPlayer(s.name)}
              className={chip(player === s.name)}
            >
              {shortName(s.name)}
              <span className="font-mono font-normal opacity-80">
                {s.made}/{s.attempts}
              </span>
            </button>
          ))}
        </div>
        <ShotCourt shots={shots} color={getTeamColor(team.abbreviation)} />
      </CardContent>
    </Card>
  );
}

/* ------------------------------------------------------------- Temps forts */

const KIND_LABELS: Record<KeyPlay["kind"], string> = {
  three: "tir à 3 pts",
  dunk: "dunk",
  alley_oop: "alley-oop",
  layup: "lay-up",
  hook: "bras roulé",
  jumper: "tir à 2 pts",
  free_throw: "lancer franc",
  end_period: "",
  end_game: "",
};

/**
 * Le fil du match en français, du plus récent au plus ancien : paniers et
 * fins de période. Écrit à partir des données de chaque action (type, joueur,
 * points), pas du texte anglais d'ESPN.
 */
export function KeyPlaysList({ match, live }: { match: Match; live: LiveMatch }) {
  const [withFreeThrows, setWithFreeThrows] = useState(false);
  const teamOf = (abbr: string | null) =>
    abbr === match.homeTeam.abbreviation ? match.homeTeam : abbr === match.awayTeam.abbreviation ? match.awayTeam : null;

  if (live.keyPlays.length === 0) {
    return <p className="text-sm text-muted-foreground">Le fil du match commence au coup d&apos;envoi.</p>;
  }

  // Une section par période, la plus récente en haut, chacune du plus récent au plus ancien.
  const periods = [...new Set(live.keyPlays.map((p) => p.period))].filter((p) => p > 0).sort((a, b) => b - a);
  const finalPlay = live.keyPlays.find((p) => p.kind === "end_game");

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-6">
        <label className="flex items-center justify-end gap-2 text-xs text-muted-foreground">
          <input type="checkbox" checked={withFreeThrows} onChange={(e) => setWithFreeThrows(e.target.checked)} />
          Afficher les lancers francs
        </label>
        {finalPlay && (
          <p className="rounded-lg bg-tint/[0.06] px-3 py-1.5 text-center text-xs font-semibold">
            Fin du match · {match.awayTeam.abbreviation} {finalPlay.awayScore} - {finalPlay.homeScore}{" "}
            {match.homeTeam.abbreviation}
          </p>
        )}
        {periods.map((period, index) => {
          const all = live.keyPlays.filter((p) => p.period === period);
          const plays = all
            .filter((p) => p.kind !== "end_period" && p.kind !== "end_game")
            .filter((p) => withFreeThrows || p.kind !== "free_throw")
            .reverse();
          const last = all[all.length - 1];
          const ended = all.some((p) => p.kind === "end_period");
          return (
            // Ouverte par défaut : la période en cours (ou la dernière jouée).
            <details key={period} open={index === 0} className="group rounded-xl bg-tint/[0.03]">
              <summary className="flex cursor-pointer list-none items-center gap-2 px-3 py-2 text-sm font-semibold">
                <ChevronDown className="h-4 w-4 shrink-0 -rotate-90 text-muted-foreground transition-transform group-open:rotate-0" />
                <span className="flex-1 first-letter:uppercase">{periodName(period)}</span>
                <span className="font-mono text-xs text-muted-foreground">
                  {ended ? "" : "en cours · "}
                  {last.awayScore}-{last.homeScore}
                </span>
              </summary>
              <ol className="flex flex-col px-3 pb-1">
                {plays.length === 0 && <li className="py-2 text-xs text-muted-foreground">Pas encore de panier.</li>}
                {plays.map((p, i) => {
                  const team = teamOf(p.teamAbbreviation);
                  return (
                    <li key={i} className="flex items-center gap-2.5 border-t border-tint/10 py-2 text-sm">
                      <span className="w-10 shrink-0 font-mono text-[11px] text-muted-foreground">{formatClock(p.clock)}</span>
                      {team && <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={18} />}
                      <span className="min-w-0 flex-1">
                        <span className="block truncate font-semibold">{shortName(p.playerName) || team?.name}</span>
                        <span className="block truncate text-xs text-muted-foreground">{KIND_LABELS[p.kind]}</span>
                      </span>
                      <span className={cn("shrink-0 font-mono text-xs", p.points === 3 && "font-bold text-primary")}>
                        +{p.points}
                      </span>
                      <span className="w-14 shrink-0 text-right font-mono text-xs tabular-nums">
                        {p.awayScore}-{p.homeScore}
                      </span>
                    </li>
                  );
                })}
              </ol>
            </details>
          );
        })}
      </CardContent>
    </Card>
  );
}
