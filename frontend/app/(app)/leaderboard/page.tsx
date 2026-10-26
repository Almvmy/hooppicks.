"use client";

import { use, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { ArrowDown, CalendarRange, Heart, Search, TrendingUp, Trophy, Users } from "lucide-react";
import { PaginationControls, usePagination } from "@/components/ui/pagination-controls";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { NetPattern } from "@/components/net-pattern";
import { LeaderboardTable } from "@/components/leaderboard-table";
import { PlayerAvatar } from "@/components/player-avatar";
import { CountUp } from "@/components/motion/count-up";
import { RankChange, RecentForm, StreakBadge } from "@/components/leaderboard/trends";
import { FanTeams } from "@/components/leaderboard/fan-teams";
import { ChampionBanner } from "@/components/leaderboard/champion-banner";
import { Podium } from "@/components/leaderboard/podium";
import { fetchPhaseChampions, fetchWeekChampions } from "@/lib/api/palmares";
import { championPhase, periodOption, useLeaderboardPeriods } from "@/lib/leaderboard-periods";
import { ShareImageButton } from "@/components/share-image-button";
import { fetchLeaderboard, leaderboardQueryKey } from "@/lib/api/leaderboard";
import { fetchProfile } from "@/lib/api/auth";
import { normalizeForSearch } from "@/lib/help-content";
import { rankTitle } from "@/lib/rank-title";
import { favoriteTeamAbbreviation, useTeamsByAbbreviation } from "@/lib/use-teams";
import { LeaderboardEntry, LeaderboardPeriod } from "@/lib/types";
import { cn, formatSignedPoints } from "@/lib/utils";

const PAGE_SIZE = 20;

function chipClass(active: boolean) {
  return cn(
    "flex shrink-0 items-center gap-1.5 rounded-full px-3 py-1.5 text-sm transition-colors",
    active ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
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
          {/* Son propre maillot, au milieu des autres : c'est là qu'on a envie de le changer. */}
          <Link href="/maillot?suite=/leaderboard" title="Modifier mon maillot" aria-label="Modifier mon maillot" className="shrink-0 transition-transform hover:scale-105">
            <PlayerAvatar number={me.avatarNumber} position={me.avatarPosition} colorway={me.avatarColorway} icon={me.avatarIcon} size="md" />
          </Link>
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
                  : "À égalité avec le joueur devant toi."}
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
  const periods = useLeaderboardPeriods();
  const { data, isLoading, isError } = useQuery({
    queryKey: leaderboardQueryKey(period),
    queryFn: () => fetchLeaderboard(period),
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
  const [query, setQuery] = useState("");
  // Champion(s) de la semaine passée : bandeau en tête et couronne à côté du pseudo.
  // Sur le mois : celui du mois passé ; sur une phase : son champion, une fois finie.
  const phase = championPhase(period);
  const championsQuery = useQuery({ queryKey: ["week-champions"], queryFn: () => fetchWeekChampions(), staleTime: 10 * 60 * 1000, enabled: !phase });
  const phaseChampionsQuery = useQuery({
    queryKey: ["phase-champions", phase],
    queryFn: () => fetchPhaseChampions(phase!),
    staleTime: 10 * 60 * 1000,
    enabled: !!phase,
  });
  const championsData = phase ? phaseChampionsQuery.data : championsQuery.data;
  const champions = new Set(championsData?.champions.map((c) => c.username));

  const all = data ?? [];
  const normalizedQuery = normalizeForSearch(query.trim());
  const isSearching = normalizedQuery.length > 0;
  const scope = periodOption(period).scope;

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
      {championsData && <ChampionBanner data={championsData} />}

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="glass-scroll edge-scroll flex items-center gap-2 overflow-x-auto pb-0.5" role="group" aria-label="Période">
          <CalendarRange className="h-4 w-4 shrink-0 text-muted-foreground" aria-hidden />
          {periods.map((p) => (
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

      {!isLoading && !isError && all.length > 0 && (
        <p className="-mt-3 text-sm text-muted-foreground">
          {all.length} joueur{all.length > 1 ? "s" : ""} classé{all.length > 1 ? "s" : ""}
        </p>
      )}

      {me && !isSearching && <MyPosition me={me} above={above} total={all.length} onShow={showMyRow} />}

      {!isLoading && !isError && username && !me && all.length > 0 && (
        <Card>
          <CardContent className="pt-6 text-sm text-muted-foreground">
            {period === "season"
              ? "Pas encore classé : ton premier ticket réglé t'y fera entrer."
              : "Aucun de tes tickets n'a été réglé sur cette période."}{" "}
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
            Personne n&apos;est encore au classement {scope}.
          </p>
          <Link href="/matches" className="text-sm font-medium text-primary hover:underline">
            Voir les matchs
          </Link>
        </div>
      )}

      {showPodium && (
        <Podium
          entries={podium}
          username={username}
          champions={champions}
          action={
            <ShareImageButton
              src={`/podium/carte?periode=${period}`}
              fileName={`hooppicks-podium-${period}.png`}
              text={`Le podium HoopPicks ${scope} 🏆 Qui va détrôner @${podium[0].username} ?`}
              discreet
            />
          }
        />
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
            champions={champions}
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
