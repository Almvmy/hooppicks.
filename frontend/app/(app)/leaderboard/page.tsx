"use client";

import { use, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { ArrowDown, CalendarRange, Crown, Heart, Search, TrendingUp, Trophy, Users } from "lucide-react";
import { PaginationControls, usePagination } from "@/components/ui/pagination-controls";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { CourtWatermark } from "@/components/court-watermark";
import { NetPattern } from "@/components/net-pattern";
import { LeaderboardTable } from "@/components/leaderboard-table";
import { PlayerAvatar } from "@/components/player-avatar";
import { TeamLogo } from "@/components/team-logo";
import { CountUp } from "@/components/motion/count-up";
import { RankChange, RecentForm, StreakBadge } from "@/components/leaderboard/trends";
import { FanTeams } from "@/components/leaderboard/fan-teams";
import { fetchLeaderboard, leaderboardQueryKey } from "@/lib/api/leaderboard";
import { fetchProfile } from "@/lib/api/auth";
import { normalizeForSearch } from "@/lib/help-content";
import { rankTitle } from "@/lib/rank-title";
import { favoriteTeamAbbreviation, useTeamsByAbbreviation } from "@/lib/use-teams";
import { LeaderboardEntry, LeaderboardPeriod } from "@/lib/types";
import { cn, formatSignedPoints } from "@/lib/utils";

const PAGE_SIZE = 20;

const PERIODS: { value: LeaderboardPeriod; label: string; scope: string }[] = [
  { value: "season", label: "Saison", scope: "de la saison" },
  { value: "month", label: "Ce mois-ci", scope: "du mois" },
  { value: "week", label: "Cette semaine", scope: "de la semaine de jeu" },
];

// Or, argent, bronze : en accent (trophée, liseré, marche du podium), jamais
// en couleur de texte, pour rester lisibles dans les deux thèmes.
const MEDALS: Record<number, { color: string; label: string }> = {
  1: { color: "#F5B700", label: "Or" },
  2: { color: "#A8B3C2", label: "Argent" },
  3: { color: "#CD7F32", label: "Bronze" },
};

function chipClass(active: boolean) {
  return cn(
    "flex shrink-0 items-center gap-1.5 rounded-full px-3 py-1.5 text-sm transition-colors",
    active ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
  );
}

function PodiumSpot({
  entry,
  place,
  teams,
  isMe,
}: {
  entry: LeaderboardEntry;
  place: 1 | 2 | 3;
  teams: ReturnType<typeof useTeamsByAbbreviation>;
  isMe: boolean;
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

function MyPosition({
  me,
  above,
  total,
  onShow,
}: {
  me: LeaderboardEntry;
  above: LeaderboardEntry | undefined;
  total: number;
  onShow: () => void;
}) {
  const gap = above ? above.points - me.points : 0;
  return (
    <Card className="glass-accent">
      <CardContent className="flex flex-wrap items-center justify-between gap-4 pt-6">
        <div className="flex items-center gap-4">
          <span className="flex flex-col items-center">
            <span className="font-heading text-4xl font-bold tabular-nums">#{me.rank}</span>
            <RankChange entry={me} />
          </span>
          <div>
            <p className="text-xs uppercase tracking-wide text-muted-foreground">
              Ta position · {rankTitle(me.rank, total)}
            </p>
            <p className="flex items-center gap-2 font-mono text-lg font-bold text-foreground">
              <CountUp value={me.points} format={(n) => formatSignedPoints(n)} />
              <StreakBadge streak={me.streak} />
            </p>
            <p className="mt-0.5 flex items-center gap-1 text-sm text-muted-foreground">
              <TrendingUp className="h-3.5 w-3.5" />
              {me.rank === 1
                ? "Tu es en tête du classement."
                : above && gap > 0
                  ? `Plus que ${gap.toLocaleString("fr-FR")} pts pour doubler ${above.username} (#${above.rank}).`
                  : "À égalité avec le joueur devant toi : un ticket gagnant suffit."}
            </p>
          </div>
        </div>
        <div className="flex flex-col items-end gap-2">
          {me.recentForm.length > 0 && (
            <span className="flex items-center gap-2 text-xs text-muted-foreground">
              Ta forme
              <RecentForm form={me.recentForm} />
            </span>
          )}
          <Button variant="outline" size="sm" onClick={onShow}>
            <ArrowDown className="h-4 w-4" />
            Voir ma ligne
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

function PlayersRanking({ username }: { username: string | undefined }) {
  const [period, setPeriod] = useState<LeaderboardPeriod>("season");
  const { data, isLoading, isError } = useQuery({
    queryKey: leaderboardQueryKey(period),
    queryFn: () => fetchLeaderboard(period),
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
  const teams = useTeamsByAbbreviation();
  const [query, setQuery] = useState("");

  const all = data ?? [];
  const normalizedQuery = normalizeForSearch(query.trim());
  const isSearching = normalizedQuery.length > 0;
  const scope = PERIODS.find((p) => p.value === period)!.scope;

  // Podium = les 3 premières places (hors recherche) ; le tableau reprend à la 4e.
  const showPodium = !isSearching && all.length > 0;
  const podium = showPodium ? all.slice(0, 3) : [];
  const tableEntries = isSearching
    ? all.filter((e) => normalizeForSearch(e.username).includes(normalizedQuery))
    : all.slice(podium.length);

  const { page, pageCount, pageItems, setPage, totalCount } = usePagination(tableEntries, PAGE_SIZE);

  const myIndex = all.findIndex((e) => e.username === username);
  const me = myIndex >= 0 ? all[myIndex] : undefined;
  const above = myIndex > 0 ? all.slice(0, myIndex).reverse().find((e) => e.points > (me?.points ?? 0)) : undefined;

  // « Voir ma ligne » : on change de page si besoin, puis on fait défiler
  // jusqu'à la ligne une fois qu'elle est rendue.
  const pendingScroll = useRef<string | null>(null);
  useEffect(() => {
    if (!pendingScroll.current) return;
    const row = document.querySelector(`[data-flip-key="${CSS.escape(pendingScroll.current)}"]`);
    row?.scrollIntoView({ behavior: "smooth", block: "center" });
    pendingScroll.current = null;
  }, [page, pageItems]);

  function showMyRow() {
    // Le bouton n'est affiché que hors recherche : la liste est complète ici.
    if (!me) return;
    const indexInTable = all.slice(3).findIndex((e) => e.username === me.username);
    pendingScroll.current = me.username;
    if (indexInTable < 0) {
      // Sur le podium : il est déjà en haut de la page.
      window.scrollTo({ top: 0, behavior: "smooth" });
      pendingScroll.current = null;
      return;
    }
    const targetPage = Math.floor(indexInTable / PAGE_SIZE) + 1;
    if (targetPage === page) {
      document.querySelector(`[data-flip-key="${CSS.escape(me.username)}"]`)?.scrollIntoView({ behavior: "smooth", block: "center" });
      pendingScroll.current = null;
    } else {
      setPage(targetPage);
    }
  }

  function choosePeriod(next: LeaderboardPeriod) {
    setPeriod(next);
    setPage(1);
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="glass-scroll edge-scroll flex items-center gap-2 overflow-x-auto pb-0.5" role="group" aria-label="Période">
          <CalendarRange className="h-4 w-4 shrink-0 text-muted-foreground" aria-hidden />
          {PERIODS.map((p) => (
            <button key={p.value} type="button" className={chipClass(period === p.value)} onClick={() => choosePeriod(p.value)}>
              {p.label}
            </button>
          ))}
        </div>
        {all.length > 3 && (
          <div className="relative w-full sm:w-64">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              type="search"
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                setPage(1);
              }}
              placeholder="Chercher un joueur"
              aria-label="Chercher un joueur dans le classement"
              className="pl-9"
            />
          </div>
        )}
      </div>

      {!isLoading && !isError && (
        <p className="-mt-3 text-sm text-muted-foreground">
          {all.length > 0
            ? `${all.length} joueur${all.length > 1 ? "s" : ""} classé${all.length > 1 ? "s" : ""} au classement ${scope}.`
            : null}
          {" "}Points : le bilan de chaque semaine (gains moins mises), jamais moins de 0
          {period === "week" ? ", depuis lundi 12h." : period === "month" ? ", semaines du mois additionnées." : ", semaines additionnées."}
        </p>
      )}

      {me && !isSearching && <MyPosition me={me} above={above} total={all.length} onShow={showMyRow} />}

      {!isLoading && !isError && username && !me && all.length > 0 && (
        <Card>
          <CardContent className="pt-6 text-sm text-muted-foreground">
            {period === "season"
              ? "Tu n'apparais pas encore au classement : il compte les tickets résolus. Dès que ton premier ticket sera gagné ou perdu, tu y entreras."
              : `Aucun de tes tickets n'a été résolu sur cette période : un ticket résolu suffit pour entrer au classement ${scope}.`}{" "}
            <Link href="/matches" className="font-medium text-primary hover:underline">
              Voir les matchs
            </Link>
          </CardContent>
        </Card>
      )}

      {!isLoading && !isError && all.length === 0 && (
        <div className="glass flex flex-col items-center gap-3 rounded-2xl py-10 text-center">
          <Trophy className="h-8 w-8 text-muted-foreground" />
          <p className="max-w-sm text-sm text-muted-foreground">
            Personne n&apos;est encore au classement {scope} : le premier ticket résolu prend la tête.
          </p>
          <Link href="/matches" className="text-sm font-medium text-primary hover:underline">
            Voir les matchs
          </Link>
        </div>
      )}

      {showPodium && (
        <div className="glass relative overflow-hidden rounded-3xl px-4 pt-10 sm:px-10">
          <CourtWatermark className="pointer-events-none absolute -right-6 -top-10 h-[220px] w-[340px] opacity-[0.05]" />
          <div className="relative mx-auto flex max-w-xl items-end gap-3 sm:gap-6">
            {podium.map((entry, i) => (
              <PodiumSpot
                key={entry.username}
                entry={entry}
                place={(i + 1) as 1 | 2 | 3}
                teams={teams}
                isMe={entry.username === username}
              />
            ))}
            {podium.length < 2 && <PodiumGhost place={2} />}
            {podium.length < 3 && <PodiumGhost place={3} />}
          </div>
        </div>
      )}

      {(isLoading || isError || tableEntries.length > 0 || isSearching) && (
        <div className="glass relative overflow-hidden rounded-2xl">
          <NetPattern className="pointer-events-none absolute -bottom-10 -left-8 h-[200px] w-[260px] opacity-[0.05]" />
          <LeaderboardTable
            entries={pageItems}
            isLoading={isLoading}
            isError={isError}
            currentUsername={username}
            showTrends
            emptyMessage={isSearching ? `Aucun joueur ne correspond à « ${query.trim()} ».` : undefined}
          />
        </div>
      )}

      {!isLoading && !isError && totalCount > PAGE_SIZE && (
        <PaginationControls page={page} pageCount={pageCount} onPageChange={setPage} />
      )}
    </div>
  );
}

/** Fans par équipe : toujours sur la saison, le classement le plus représentatif. */
function FansRanking({ favoriteTeam }: { favoriteTeam: string | undefined }) {
  const { data, isLoading, isError } = useQuery({
    queryKey: leaderboardQueryKey("season"),
    queryFn: () => fetchLeaderboard("season"),
    staleTime: 2 * 60 * 1000,
  });
  const teams = useTeamsByAbbreviation();

  if (isLoading) return <p className="text-sm text-muted-foreground">Chargement…</p>;
  if (isError || !data) return <p className="text-destructive">Impossible de charger le classement.</p>;
  return <FanTeams entries={data} myTeam={favoriteTeamAbbreviation(favoriteTeam, teams)} />;
}

type View = "joueurs" | "fans";

const VIEWS: { value: View; label: string; icon: typeof Users }[] = [
  { value: "joueurs", label: "Joueurs", icon: Users },
  { value: "fans", label: "Fans par équipe", icon: Heart },
];

export default function LeaderboardPage({ searchParams }: { searchParams: Promise<{ vue?: string }> }) {
  // Onglet dans l'adresse (?vue=fans) : retrouvé au retour sur la page et partageable.
  const { vue } = use(searchParams);
  const router = useRouter();
  const [view, setViewState] = useState<View>(VIEWS.some((v) => v.value === vue) ? (vue as View) : "joueurs");
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });

  // Ancienne vue « Mes ligues », devenue l'onglet Ligues de la rubrique.
  useEffect(() => {
    if (vue === "ligues") router.replace("/leagues");
  }, [vue, router]);

  function setView(next: View) {
    setViewState(next);
    router.replace(next === "joueurs" ? "/leaderboard" : `/leaderboard?vue=${next}`, { scroll: false });
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-bold">Classement</h1>
        <p className="mt-1 text-muted-foreground">Les meilleurs pronostiqueurs, leurs séries et leurs communautés.</p>
      </div>

      <div className="glass-scroll edge-scroll flex gap-2 overflow-x-auto pb-0.5" role="tablist">
        {VIEWS.map(({ value, label, icon: Icon }) => (
          <button
            key={value}
            type="button"
            role="tab"
            aria-selected={view === value}
            className={chipClass(view === value)}
            onClick={() => setView(value)}
          >
            <Icon className="h-3.5 w-3.5" />
            {label}
          </button>
        ))}
      </div>

      {view === "joueurs" && <PlayersRanking username={profile?.username} />}
      {view === "fans" && <FansRanking favoriteTeam={profile?.favoriteTeam} />}
    </div>
  );
}
