"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowDown, Crown, Search, TrendingUp, Trophy } from "lucide-react";
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
import { fetchLeaderboard } from "@/lib/api/leaderboard";
import { fetchProfile } from "@/lib/api/auth";
import { normalizeForSearch } from "@/lib/help-content";
import { rankTitle } from "@/lib/rank-title";
import { favoriteTeamAbbreviation, useTeamsByAbbreviation } from "@/lib/use-teams";
import { LeaderboardEntry } from "@/lib/types";
import { cn } from "@/lib/utils";

const PAGE_SIZE = 20;

// Or, argent, bronze : en accent (trophée, liseré, marche du podium), jamais
// en couleur de texte, pour rester lisibles dans les deux thèmes.
const MEDALS: Record<number, { color: string; label: string }> = {
  1: { color: "#F5B700", label: "Or" },
  2: { color: "#A8B3C2", label: "Argent" },
  3: { color: "#CD7F32", label: "Bronze" },
};

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
      </div>
      <span className="flex max-w-full items-center gap-1.5 font-heading text-sm font-bold group-hover:underline sm:text-base">
        <span className="truncate">{entry.username}</span>
        {favorite && <TeamLogo abbreviation={favorite} logoUrl={teams.get(favorite)?.logoUrl} size={18} />}
      </span>
      {isMe && <span className="text-[11px] font-semibold text-primary">C&apos;est toi</span>}
      <span className="mt-0.5 font-mono text-sm font-bold">
        <CountUp value={entry.points} format={(n) => `${n.toLocaleString("fr-FR")} pts`} />
      </span>
      <span className="text-xs text-muted-foreground">{entry.winRate}% de réussite</span>

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
          <span className="font-heading text-4xl font-bold tabular-nums">#{me.rank}</span>
          <div>
            <p className="text-xs uppercase tracking-wide text-muted-foreground">
              Ta position · {rankTitle(me.rank, total)}
            </p>
            <p className="font-mono text-lg font-bold text-foreground">
              <CountUp value={me.points} format={(n) => `${n.toLocaleString("fr-FR")} pts`} />
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
        <Button variant="outline" size="sm" onClick={onShow}>
          <ArrowDown className="h-4 w-4" />
          Voir ma ligne
        </Button>
      </CardContent>
    </Card>
  );
}

export default function LeaderboardPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["leaderboard"],
    queryFn: fetchLeaderboard,
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
  const { data: profile } = useQuery({ queryKey: ["profile"], queryFn: fetchProfile });
  const teams = useTeamsByAbbreviation();
  const [query, setQuery] = useState("");

  const all = data ?? [];
  const normalizedQuery = normalizeForSearch(query.trim());
  const isSearching = normalizedQuery.length > 0;

  // Podium = les 3 premières places (hors recherche) ; le tableau reprend à la 4e.
  const showPodium = !isSearching && all.length > 0;
  const podium = showPodium ? all.slice(0, 3) : [];
  const tableEntries = isSearching
    ? all.filter((e) => normalizeForSearch(e.username).includes(normalizedQuery))
    : all.slice(podium.length);

  const { page, pageCount, pageItems, setPage, totalCount } = usePagination(tableEntries, PAGE_SIZE);

  const myIndex = all.findIndex((e) => e.username === profile?.username);
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

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-heading text-2xl font-bold">Classement</h1>
          <p className="mt-1 text-muted-foreground">
            Les meilleurs pronostiqueurs de la saison
            {!isLoading && !isError && all.length > 0 && ` : ${all.length} joueurs classés`}.
          </p>
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

      {me && !isSearching && <MyPosition me={me} above={above} total={all.length} onShow={showMyRow} />}

      {!isLoading && !isError && profile && !me && (
        <Card>
          <CardContent className="pt-6 text-sm text-muted-foreground">
            Tu n&apos;apparais pas encore au classement : il compte les tickets résolus. Dès que ton premier ticket
            sera gagné ou perdu, tu y entreras.{" "}
            <Link href="/matches" className="font-medium text-primary hover:underline">
              Voir les matchs
            </Link>
          </CardContent>
        </Card>
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
                isMe={entry.username === profile?.username}
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
            currentUsername={profile?.username}
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
