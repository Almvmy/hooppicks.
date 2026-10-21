"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Check, ChevronRight, Lock, Medal, Play, Search, Sparkles, Trophy, Users, X } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { TeamLogo } from "@/components/team-logo";
import { ShareButton } from "@/components/share-button";
import { ShareImageButton } from "@/components/share-image-button";
import { SeasonStory } from "@/components/season-story";
import { fetchProfile } from "@/lib/api/auth";
import { fetchSeasonPicks, saveSeasonPick, SeasonPicksOverview, SeasonQuestion } from "@/lib/api/season-picks";
import { countdownParts, useNow, useSeasonLaunch } from "@/lib/season-launch";
import { CONFERENCE_OF, formatChance, seasonChances } from "@/lib/season-odds";
import { getTeamColor } from "@/lib/team-colors";
import { useTeamsByAbbreviation } from "@/lib/use-teams";
import { TeamRank } from "@/lib/types";
import { cn, formatMatchDate, formatMatchTime } from "@/lib/utils";
import { normalizeForSearch } from "@/lib/help-content";

const REGISTER_HREF = `/register?from=${encodeURIComponent("/saison")}`;

function Countdown({ deadline, now }: { deadline: Date; now: number }) {
  const { days, hours, minutes, seconds } = countdownParts(deadline.getTime() - now);
  return (
    <div className="grid max-w-sm grid-cols-4 gap-2" role="timer" aria-live="off">
      {[
        { v: days, l: "jours" },
        { v: hours, l: "heures" },
        { v: minutes, l: "min" },
        { v: seconds, l: "s" },
      ].map(({ v, l }) => (
        <div key={l} className="rounded-xl bg-white/[0.06] px-2 py-2 text-center ring-1 ring-white/10">
          <div className="font-mono text-xl font-bold tabular-nums sm:text-2xl">{String(v).padStart(2, "0")}</div>
          <div className="text-[10px] font-semibold uppercase tracking-wider text-jersey-muted">{l}</div>
        </div>
      ))}
    </div>
  );
}

function ProgressRing({ done, total }: { done: number; total: number }) {
  const r = 26;
  const c = 2 * Math.PI * r;
  return (
    <div className="relative h-16 w-16 shrink-0">
      <svg viewBox="0 0 64 64" className="h-16 w-16 -rotate-90">
        <circle cx="32" cy="32" r={r} fill="none" stroke="rgba(255,255,255,0.12)" strokeWidth="6" />
        <circle
          cx="32"
          cy="32"
          r={r}
          fill="none"
          stroke="var(--brand)"
          strokeWidth="6"
          strokeLinecap="round"
          strokeDasharray={c}
          strokeDashoffset={c * (1 - done / total)}
          className="transition-[stroke-dashoffset] duration-700"
        />
      </svg>
      <span className="absolute inset-0 flex items-center justify-center font-mono text-sm font-bold">
        {done}/{total}
      </span>
    </div>
  );
}

function Hero({
  data,
  anonymous,
  username,
  now,
  onReplayIntro,
}: {
  data: SeasonPicksOverview;
  anonymous: boolean;
  username: string | undefined;
  now: number;
  onReplayIntro: (() => void) | null;
}) {
  const deadline = data.deadline ? new Date(data.deadline) : null;
  const done = Object.keys(data.myPicks).length;
  const total = data.questions.reduce((sum, q) => sum + q.points, 0);
  const shareSrc = useMemo(() => {
    const p = new URLSearchParams({ s: data.season, ...data.myPicks });
    if (username) p.set("u", username);
    return `/saison/carte?${p.toString()}`;
  }, [data.season, data.myPicks, username]);

  return (
    <section className="island relative overflow-hidden rounded-2xl bg-jersey text-jersey-foreground shadow-[var(--lift-strong)]">
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0"
        style={{
          background:
            "radial-gradient(60% 90% at 100% 0%, color-mix(in srgb, var(--brand) 34%, transparent), transparent 70%), radial-gradient(50% 70% at 0% 100%, color-mix(in srgb, var(--paint) 28%, transparent), transparent 70%)",
        }}
      />
      <Trophy aria-hidden className="pointer-events-none absolute -right-6 -top-4 h-44 w-44 rotate-12 text-white/[0.05]" />

      <div className="relative flex flex-col gap-5 p-5 sm:p-6">
        <span className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-[0.18em] text-brand">
          <Sparkles className="h-4 w-4" />
          Saison {data.season}
        </span>
        <div>
          <h1 className="font-heading text-3xl font-black italic uppercase leading-none sm:text-4xl">Pronostics de saison</h1>
          <p className="mt-2 max-w-xl text-sm text-jersey-foreground/80">
            Champion, finalistes, meilleur bilan : quatre choix pour toute la saison, jusqu&apos;à {total} pts à la clé.
            Classement à part, il ne touche pas aux points de la semaine.
          </p>
        </div>

        {!data.locked && deadline && deadline.getTime() > now && (
          <div className="flex flex-col gap-2">
            <span className="text-[11px] font-bold uppercase tracking-wider text-jersey-muted">
              Clôture le {formatMatchDate(deadline)} à {formatMatchTime(deadline)}
            </span>
            <Countdown deadline={deadline} now={now} />
          </div>
        )}
        {data.locked && (
          <p className="flex items-center gap-2 text-sm font-semibold">
            <Lock className="h-4 w-4 text-brand" />
            Pronostics clos : les choix de tous les joueurs sont révélés.
          </p>
        )}

        <div className="flex flex-wrap items-center gap-4">
          {!anonymous && <ProgressRing done={done} total={data.questions.length} />}
          {data.participants > 0 && (
            <span className="flex items-center gap-2 text-sm text-jersey-foreground/90">
              <Users className="h-4 w-4 text-brand" />
              {data.participants.toLocaleString("fr-FR")} joueur{data.participants > 1 ? "s ont" : " a"} déjà pronostiqué
            </span>
          )}
        </div>

        <div className="flex flex-wrap gap-2">
          {anonymous ? (
            <Link
              href={REGISTER_HREF}
              className="inline-flex items-center gap-1.5 rounded-xl bg-brand px-4 py-2.5 text-sm font-bold text-brand-foreground transition-transform hover:-translate-y-0.5"
            >
              Créer mon compte pour pronostiquer
              <ChevronRight className="h-4 w-4" />
            </Link>
          ) : (
            done > 0 && (
              <ShareImageButton
                src={shareSrc}
                fileName={`pronos-saison-${data.season}.png`}
                text="Mes pronostics de saison sur HoopPicks. Et toi, qui sera champion ?"
                label="Partager mes pronos"
              />
            )
          )}
          <ShareButton
            path="/saison"
            text={data.locked ? "Les pronostics de saison HoopPicks sont révélés :" : "Qui sera champion NBA ? Fais tes pronostics de saison sur HoopPicks :"}
            label={data.locked ? "Partager la page" : "Inviter à pronostiquer"}
          />
          {onReplayIntro && (
            <button
              type="button"
              onClick={onReplayIntro}
              className="inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-semibold text-jersey-foreground/85 ring-1 ring-white/15 transition-colors hover:bg-white/10"
            >
              <Play className="h-3.5 w-3.5 text-brand" />
              Revoir l&apos;intro de la saison
            </button>
          )}
        </div>
        {anonymous && (
          <p className="-mt-2 text-xs text-jersey-muted">
            Déjà un compte ?{" "}
            <Link href={`/login?from=${encodeURIComponent("/saison")}`} className="font-semibold text-brand hover:underline">
              Se connecter
            </Link>
          </p>
        )}
      </div>
    </section>
  );
}

function TeamPicker({
  question,
  teams,
  selected,
  pending,
  onPick,
  onClose,
}: {
  question: SeasonQuestion | null;
  teams: TeamRank[];
  selected: string | undefined;
  pending: boolean;
  onPick: (abbr: string) => void;
  onClose: () => void;
}) {
  const [query, setQuery] = useState("");
  const chances = useMemo(() => (question ? seasonChances(question.key, teams) : []), [question, teams]);
  const q = normalizeForSearch(query.trim());
  const shown = q
    ? chances.filter(({ team }) => normalizeForSearch(`${team.name} ${team.abbreviation}`).includes(q))
    : chances;
  const max = chances[0]?.pct ?? 1;

  return (
    <Dialog
      open={question !== null}
      onOpenChange={(open) => {
        if (!open) {
          setQuery("");
          onClose();
        }
      }}
    >
      {question && (
        <DialogContent className="flex max-h-[85dvh] max-w-md flex-col gap-3 overflow-hidden">
          <DialogTitle>{question.label}</DialogTitle>
          <DialogDescription>
            {question.points} pts si c&apos;est juste. Équipes classées selon l&apos;estimation HoopPicks (d&apos;après l&apos;Elo).
          </DialogDescription>
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Rechercher une équipe"
              aria-label="Rechercher une équipe"
              className="pl-9"
            />
          </div>
          <ul className="-mx-1 flex min-h-0 flex-1 flex-col gap-1 overflow-y-auto overscroll-contain px-1 pb-1">
            {shown.map(({ team, pct }, i) => {
              const isSelected = selected === team.abbreviation;
              const color = getTeamColor(team.abbreviation);
              return (
                <li key={team.abbreviation}>
                  <button
                    type="button"
                    disabled={pending}
                    aria-pressed={isSelected}
                    onClick={() => onPick(team.abbreviation)}
                    className={cn(
                      "relative flex w-full items-center gap-3 overflow-hidden rounded-xl px-3 py-2 text-left transition-colors",
                      isSelected ? "glass-accent" : "glass-inset-quiet hover:bg-tint/[0.06]"
                    )}
                  >
                    <span
                      aria-hidden
                      className="absolute inset-y-0 left-0 opacity-25"
                      style={{ width: `${Math.max(4, (pct / max) * 100)}%`, background: `linear-gradient(90deg, ${color}, transparent)` }}
                    />
                    <span className="relative w-5 shrink-0 text-center font-mono text-xs text-muted-foreground">{q ? "" : i + 1}</span>
                    <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={30} />
                    <span className="relative min-w-0 flex-1">
                      <span className="block truncate font-semibold">{team.name}</span>
                    </span>
                    <span className="relative shrink-0 font-mono text-sm font-bold">{formatChance(pct)}</span>
                    {isSelected && <Check className="relative h-4 w-4 shrink-0 text-primary" />}
                  </button>
                </li>
              );
            })}
            {shown.length === 0 && <li className="py-6 text-center text-sm text-muted-foreground">Aucune équipe trouvée.</li>}
          </ul>
        </DialogContent>
      )}
    </Dialog>
  );
}

function QuestionCard({
  question,
  data,
  teams,
  anonymous,
  onOpen,
}: {
  question: SeasonQuestion;
  data: SeasonPicksOverview;
  teams: Map<string, TeamRank>;
  anonymous: boolean;
  onOpen: () => void;
}) {
  const mine = data.myPicks[question.key];
  const result = data.results[question.key];
  const team = mine ? teams.get(mine) : undefined;
  const chance = team ? seasonChances(question.key, [...teams.values()]).find((c) => c.team.abbreviation === team.abbreviation) : undefined;
  const color = team ? getTeamColor(team.abbreviation) : undefined;
  const split = data.community[question.key];
  const conference = CONFERENCE_OF[question.key];
  const canPick = !data.locked;

  const body = (
    <>
      {color && (
        <div
          aria-hidden
          className="pointer-events-none absolute inset-0"
          style={{ background: `radial-gradient(80% 120% at 100% 0%, color-mix(in srgb, ${color} 26%, transparent), transparent 65%)` }}
        />
      )}
      <div className="relative flex items-start justify-between gap-3">
        <div>
          <h2 className="font-heading text-base font-bold">{question.label}</h2>
          <p className="text-xs text-muted-foreground">
            {question.points} pts{conference ? ` · conférence ${conference}` : ""}
          </p>
        </div>
        {result ? (
          mine === result ? (
            <span className="flex items-center gap-1 text-sm font-semibold text-success">
              <Check className="h-4 w-4" /> +{question.points}
            </span>
          ) : (
            <span className="flex items-center gap-1 text-sm text-muted-foreground">
              <X className="h-4 w-4" /> {result}
            </span>
          )
        ) : (
          data.locked && <Lock className="h-4 w-4 text-muted-foreground" />
        )}
      </div>

      <div className="relative flex items-center gap-4">
        {team ? (
          <>
            <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={64} />
            <div className="min-w-0 flex-1">
              <p className="truncate font-heading text-xl font-black italic uppercase">{team.name}</p>
              {chance && <p className="text-xs text-muted-foreground">Estimation HoopPicks : {formatChance(chance.pct)}</p>}
            </div>
          </>
        ) : (
          <>
            <span className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full border-2 border-dashed border-tint/20 text-2xl font-black text-muted-foreground">
              ?
            </span>
            <p className="flex-1 text-sm text-muted-foreground">
              {data.locked ? "Pas de pronostic." : anonymous ? "Crée ton compte pour choisir." : "Pas encore choisi."}
            </p>
          </>
        )}
        {canPick && (
          <span className="shrink-0 rounded-lg bg-primary/10 px-3 py-1.5 text-sm font-semibold text-primary">
            {mine ? "Changer" : "Choisir"}
          </span>
        )}
      </div>

      {split && Object.keys(split).length > 0 && (
        <div className="relative flex flex-col gap-1.5">
          {Object.entries(split)
            .slice(0, 4)
            .map(([abbr, pct]) => {
              const t = teams.get(abbr);
              const isMine = abbr === mine;
              return (
                <div key={abbr} className="relative flex items-center gap-2 overflow-hidden rounded-lg px-2 py-1 text-xs">
                  <span
                    aria-hidden
                    className={cn("absolute inset-y-0 left-0 rounded-lg", isMine ? "bg-primary/25" : "bg-tint/[0.07]")}
                    style={{ width: `${Math.max(3, pct)}%` }}
                  />
                  <TeamLogo abbreviation={abbr} logoUrl={t?.logoUrl ?? null} size={18} />
                  <span className="relative flex-1 font-semibold">
                    {t?.name ?? abbr}
                    {isMine && <span className="ml-1.5 font-normal text-primary">toi</span>}
                  </span>
                  <span className="relative font-mono font-bold">{pct} %</span>
                </div>
              );
            })}
          {mine && split[mine] !== undefined && (
            <p className="text-xs text-muted-foreground">
              {split[mine] >= 50
                ? `Tu es avec la majorité : ${split[mine]} % pensent comme toi.`
                : `${split[mine]} % pensent comme toi${split[mine] <= 10 ? " : un vrai pari." : "."}`}
            </p>
          )}
        </div>
      )}
    </>
  );

  const className = "relative flex flex-col gap-3 overflow-hidden rounded-2xl p-4 sm:p-5";
  if (!canPick) {
    return (
      <Card className="overflow-hidden">
        <CardContent className={className}>{body}</CardContent>
      </Card>
    );
  }
  return anonymous ? (
    <Link href={REGISTER_HREF} className={cn("glass block transition-transform hover:-translate-y-0.5", className)}>
      {body}
    </Link>
  ) : (
    <button type="button" onClick={onOpen} className={cn("glass text-left transition-transform hover:-translate-y-0.5", className)}>
      {body}
    </button>
  );
}

export default function SeasonPicksPage() {
  const queryClient = useQueryClient();
  const now = useNow();
  const teamsMap = useTeamsByAbbreviation();
  const teams = useMemo(() => [...teamsMap.values()], [teamsMap]);
  const profileQuery = useQuery({ queryKey: ["profile"], queryFn: fetchProfile, retry: false });
  // Sans compte, /auth/me répond 401 : dès le premier échec (la barre du haut
  // relance la même requête plusieurs fois), sans attendre la fin des essais.
  const anonymous = !profileQuery.data && (profileQuery.isError || profileQuery.failureCount > 0);
  const { data, isLoading, isError } = useQuery({ queryKey: ["season-picks"], queryFn: fetchSeasonPicks });
  const [openKey, setOpenKey] = useState<string | null>(null);
  const [storyOpen, setStoryOpen] = useState(false);
  const launch = useSeasonLaunch();
  const introAvailable = !anonymous && !!launch && now >= new Date(launch.openingNightStart).getTime();

  const mutation = useMutation({
    mutationFn: ({ question, team }: { question: string; team: string }) => saveSeasonPick(question, team),
    onSuccess: (_d, { question, team }) => {
      setOpenKey(null);
      queryClient.setQueryData<SeasonPicksOverview>(["season-picks"], (old) =>
        old ? { ...old, myPicks: { ...old.myPicks, [question]: team } } : old
      );
      queryClient.invalidateQueries({ queryKey: ["season-picks"] });
      const done = Object.keys({ ...data?.myPicks, [question]: team }).length;
      if (data && done === data.questions.length) toast.success("Tes 4 pronostics sont faits. Partage-les !");
    },
    onError: (error) => toast.error(error instanceof Error && error.message ? error.message : "Pronostic non enregistré."),
  });

  const openQuestion = data?.questions.find((q) => q.key === openKey) ?? null;
  const revealed = data?.locked && Object.keys(data.community).length > 0;

  return (
    <div className="flex flex-col gap-6">
      {isError && <p className="text-destructive">Impossible de charger les pronostics de saison. Réessaie plus tard.</p>}
      {isLoading && <Skeleton className="h-72 w-full rounded-2xl" />}

      {data && (
        <>
          <Hero
            data={data}
            anonymous={anonymous}
            username={profileQuery.data?.username}
            now={now}
            onReplayIntro={introAvailable ? () => setStoryOpen(true) : null}
          />
          <SeasonStory open={storyOpen} onClose={() => setStoryOpen(false)} />

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            {data.questions.map((q) => (
              <QuestionCard
                key={q.key}
                question={q}
                data={data}
                teams={teamsMap}
                anonymous={anonymous}
                onOpen={() => setOpenKey(q.key)}
              />
            ))}
          </div>

          {!data.locked && (
            <p className="flex items-start gap-2 text-sm text-muted-foreground">
              <Sparkles className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
              <span>
                Les choix de chacun restent secrets jusqu&apos;à la clôture, puis sont révélés à tous : qui voit qui champion, et
                combien pensent comme toi.
              </span>
            </p>
          )}

          {revealed && (
            <section className="flex flex-col items-start gap-3">
              <h2 className="flex items-center gap-2 font-heading text-lg font-bold">
                <Medal className="h-5 w-5 text-primary" />
                La révélation
              </h2>
              <p className="text-sm text-muted-foreground">
                Les choix des {data.participants.toLocaleString("fr-FR")} joueurs, question par question, sont sur chaque carte
                ci-dessus.
              </p>
              <ShareImageButton
                src="/saison/revelation/carte"
                fileName={`revelation-saison-${data.season}.png`}
                text="Les pronostics de saison HoopPicks sont révélés. Qui voit qui champion ?"
                label="Partager la révélation"
              />
            </section>
          )}

          {data.leaderboard.length > 0 && (
            <section className="flex flex-col gap-3">
              <h2 className="font-heading text-sm font-bold uppercase tracking-wide text-muted-foreground">
                Classement des pronostics de saison
              </h2>
              <Card>
                <CardContent className="divide-y divide-tint/10">
                  {data.leaderboard.map((s, i) => (
                    <div key={s.username} className="flex items-center justify-between gap-3 py-2">
                      <span className="flex items-center gap-3">
                        <span className="w-6 font-mono text-sm text-muted-foreground">{i + 1}</span>
                        <Link href={`/u/${encodeURIComponent(s.username)}`} className="font-semibold hover:underline">
                          @{s.username}
                        </Link>
                      </span>
                      <span className="font-mono text-sm">
                        <span className="font-bold">{s.points} pts</span>
                        <span className="ml-2 text-muted-foreground">
                          {s.correct} juste{s.correct > 1 ? "s" : ""}
                        </span>
                      </span>
                    </div>
                  ))}
                </CardContent>
              </Card>
            </section>
          )}

          <TeamPicker
            question={openQuestion}
            teams={teams}
            selected={openKey ? data.myPicks[openKey] : undefined}
            pending={mutation.isPending}
            onPick={(abbr) => openKey && mutation.mutate({ question: openKey, team: abbr })}
            onClose={() => setOpenKey(null)}
          />
        </>
      )}
    </div>
  );
}
