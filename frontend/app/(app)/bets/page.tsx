"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Check, Clock, Layers, Minus, Radio, Ticket, TrendingDown, TrendingUp, Undo2, X } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { BasketballLoader } from "@/components/ui/basketball-loader";
import { PaginationControls, usePagination } from "@/components/ui/pagination-controls";
import { CountUp } from "@/components/motion/count-up";
import { fetchBets } from "@/lib/api/bets";
import { SelectionTeamLogo, useMatchesById } from "@/components/selection-team-logo";
import { LegState, legState, netResult } from "@/lib/bet-legs";
import { BetSelection, BetStatus, Match, PlacedBet } from "@/lib/types";
import { cn, formatLongDate, formatMatchDate, formatMatchTime } from "@/lib/utils";

const PAGE_SIZE = 8;

const STATUS_CONFIG: Record<BetStatus, { label: string; variant: "secondary" | "success" | "destructive"; strip: string }> = {
  pending: { label: "En attente", variant: "secondary", strip: "bg-live" },
  won: { label: "Gagné", variant: "success", strip: "bg-success" },
  lost: { label: "Perdu", variant: "destructive", strip: "bg-destructive" },
  void: { label: "Remboursé", variant: "secondary", strip: "bg-muted-foreground/50" },
};

type Filter = "all" | BetStatus;
const FILTERS: { value: Filter; label: string }[] = [
  { value: "all", label: "Tous" },
  { value: "pending", label: "En attente" },
  { value: "won", label: "Gagnés" },
  { value: "lost", label: "Perdus" },
  { value: "void", label: "Remboursés" },
];

// Icône + libellé : l'état ne repose jamais sur la couleur seule.
const LEG_DISPLAY: Record<LegState, { icon: React.ElementType; label: string; className: string }> = {
  won: { icon: Check, label: "Gagnée", className: "bg-success/15 text-success" },
  lost: { icon: X, label: "Perdue", className: "bg-destructive/15 text-destructive" },
  push: { icon: Undo2, label: "Remboursée", className: "bg-tint/10 text-muted-foreground" },
  winning: { icon: TrendingUp, label: "Bien parti", className: "bg-success/10 text-success" },
  losing: { icon: TrendingDown, label: "Mal parti", className: "bg-destructive/10 text-destructive" },
  level: { icon: Minus, label: "À égalité", className: "bg-tint/10 text-muted-foreground" },
  upcoming: { icon: Clock, label: "À venir", className: "bg-tint/10 text-muted-foreground" },
  unknown: { icon: Clock, label: "", className: "hidden" },
};
// Ticket remboursé (égalité ou annulation par un admin) : ses sélections ne
// comptent plus, quel que soit le résultat du match.
const CANCELLED_DISPLAY = { icon: Undo2, label: "Annulée", className: "bg-tint/10 text-muted-foreground" };

function MatchState({ match }: { match: Match | undefined }) {
  if (!match) return null;
  const d = new Date(match.date);
  if (match.status === "scheduled") {
    return (
      <span className="font-mono text-[11px] text-muted-foreground">
        {formatMatchDate(d)} · {formatMatchTime(d)}
      </span>
    );
  }
  return (
    <span className="flex items-center gap-1 font-mono text-[11px] text-muted-foreground">
      {match.status === "live" && <Radio className="h-3 w-3 animate-pulse text-live" />}
      {match.awayTeam.abbreviation} {match.awayScore}–{match.homeScore} {match.homeTeam.abbreviation}
      {match.status === "finished" ? " · final" : " · en direct"}
    </span>
  );
}

function SelectionRow({
  selection,
  matchesById,
  cancelled,
}: {
  selection: BetSelection;
  matchesById: Map<string, Match>;
  cancelled: boolean;
}) {
  const match = matchesById.get(selection.matchId);
  const state = legState(selection, match);
  const display = cancelled ? CANCELLED_DISPLAY : LEG_DISPLAY[state];
  const Icon = display.icon;
  return (
    <div className="flex items-center justify-between gap-3">
      <div className="flex min-w-0 items-center gap-3">
        <SelectionTeamLogo selection={selection} matchesById={matchesById} size={32} />
        <div className="min-w-0">
          <p className="truncate text-sm font-medium">{selection.label}</p>
          <p className="truncate text-xs text-muted-foreground">{selection.matchLabel}</p>
          <MatchState match={match} />
        </div>
      </div>
      <div className="flex shrink-0 flex-col items-end gap-1">
        <span className="font-mono text-sm font-bold">{selection.odds.toFixed(2)}</span>
        {display.label && (
          <span className={cn("flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold", display.className)}>
            <Icon className="h-3 w-3" />
            {display.label}
          </span>
        )}
      </div>
    </div>
  );
}

function TicketCard({ bet, matchesById }: { bet: PlacedBet; matchesById: Map<string, Match> }) {
  const config = STATUS_CONFIG[bet.status];
  const isParlay = bet.selections.length > 1;
  const states = bet.selections.map((s) => legState(s, matchesById.get(s.matchId)));
  const settledWins = states.filter((s) => s === "won" || s === "push").length;
  const net = netResult(bet);

  return (
    <Card className="relative overflow-hidden">
      <span aria-hidden className={cn("absolute inset-y-0 left-0 w-1", config.strip)} />
      <CardContent className="flex flex-col gap-3 pl-6 pt-5">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Badge variant={config.variant}>{config.label}</Badge>
            {isParlay && (
              <span className="flex items-center gap-1 rounded-full bg-paint/10 px-2 py-0.5 text-[11px] font-semibold text-paint">
                <Layers className="h-3 w-3" />
                Combiné ×{bet.selections.length}
              </span>
            )}
          </div>
          <span className="font-mono text-xs text-muted-foreground">{formatLongDate(new Date(bet.placedAt))}</span>
        </div>

        <div className="flex flex-col gap-3">
          {bet.selections.map((s) => (
            <SelectionRow key={s.id} selection={s} matchesById={matchesById} cancelled={bet.status === "void"} />
          ))}
        </div>

        {bet.status === "pending" && isParlay && (
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-tint/10">
              <div
                className="h-full rounded-full bg-success transition-[width] duration-700"
                style={{ width: `${(settledWins / bet.selections.length) * 100}%` }}
              />
            </div>
            {settledWins}/{bet.selections.length} validée{settledWins > 1 ? "s" : ""}
          </div>
        )}

        <div className="glass-hairline-t flex flex-wrap items-center justify-between gap-2 pt-3 text-sm">
          <span className="text-muted-foreground">
            Mise <span className="font-mono text-foreground">{bet.stake.toLocaleString("fr-FR")}</span> · cote{" "}
            <span className="font-mono text-foreground">{bet.totalOdds.toFixed(2)}</span>
          </span>
          {bet.status === "pending" && (
            <span className="text-muted-foreground">
              Gain potentiel <span className="font-mono font-bold text-foreground">{bet.potentialPayout.toLocaleString("fr-FR")} pts</span>
            </span>
          )}
          {bet.status === "won" && (
            <span className="font-mono font-bold text-success">
              +{bet.potentialPayout.toLocaleString("fr-FR")} pts
              <span className="ml-1.5 text-xs font-normal text-muted-foreground">(+{net.toLocaleString("fr-FR")} net)</span>
            </span>
          )}
          {bet.status === "lost" && (
            <span className="font-mono font-bold text-destructive">−{bet.stake.toLocaleString("fr-FR")} pts</span>
          )}
          {bet.status === "void" && <span className="text-muted-foreground">Mise rendue</span>}
        </div>
      </CardContent>
    </Card>
  );
}

function SummaryTile({ label, children, hint }: { label: string; children: React.ReactNode; hint?: string }) {
  return (
    <div className="glass rounded-2xl px-4 py-3">
      <p className="text-[11px] uppercase tracking-wide text-muted-foreground">{label}</p>
      <p className="font-heading text-2xl font-bold">{children}</p>
      {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
    </div>
  );
}

export default function BetsPage() {
  const { data, isLoading, isError } = useQuery({ queryKey: ["bets"], queryFn: fetchBets });
  const matchesById = useMatchesById();
  const [filter, setFilter] = useState<Filter>("all");

  const bets = data ?? [];
  const counts: Record<Filter, number> = {
    all: bets.length,
    pending: bets.filter((b) => b.status === "pending").length,
    won: bets.filter((b) => b.status === "won").length,
    lost: bets.filter((b) => b.status === "lost").length,
    void: bets.filter((b) => b.status === "void").length,
  };
  const resolved = counts.won + counts.lost;
  const winRate = resolved === 0 ? 0 : Math.round((counts.won / resolved) * 100);
  const net = bets.reduce((sum, b) => sum + netResult(b), 0);
  const inPlay = bets.filter((b) => b.status === "pending");
  const inPlayStake = inPlay.reduce((sum, b) => sum + b.stake, 0);
  const inPlayPotential = inPlay.reduce((sum, b) => sum + b.potentialPayout, 0);

  const filtered = filter === "all" ? bets : bets.filter((b) => b.status === filter);
  const { page, pageCount, pageItems, setPage } = usePagination(filtered, PAGE_SIZE);

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="flex items-center gap-2.5 font-heading text-2xl font-bold">
          <Ticket className="h-7 w-7 text-primary" />
          Mes paris
        </h1>
        <p className="mt-1 text-muted-foreground">Tes tickets, leur avancement match par match et ton bilan.</p>
      </div>

      {isLoading && <BasketballLoader label="Chargement de tes tickets..." />}
      {isError && <p className="text-destructive">Impossible de charger tes paris.</p>}

      {!isLoading && !isError && bets.length === 0 && (
        <Card>
          <CardContent className="flex flex-col items-center gap-2 py-10 text-center text-muted-foreground">
            <Ticket className="h-8 w-8 text-primary" />
            <p className="font-medium text-foreground">Aucun ticket pour l&apos;instant</p>
            <p className="text-sm">Choisis une cote sur un match à venir pour lancer ton premier pronostic.</p>
            <Link href="/matches" className="mt-1 text-sm font-medium text-primary hover:underline">
              Voir les matchs
            </Link>
          </CardContent>
        </Card>
      )}

      {!isLoading && !isError && bets.length > 0 && (
        <>
          <div className="stagger-children grid grid-cols-2 gap-3 lg:grid-cols-4">
            <SummaryTile label="Tickets joués" hint={`${resolved} résolu${resolved > 1 ? "s" : ""}`}>
              <CountUp value={bets.length} />
            </SummaryTile>
            <SummaryTile label="Réussite" hint={`${counts.won} gagné${counts.won > 1 ? "s" : ""} · ${counts.lost} perdu${counts.lost > 1 ? "s" : ""}`}>
              <CountUp value={winRate} format={(n) => `${n}%`} />
            </SummaryTile>
            <SummaryTile label="Bilan net" hint="gains moins mises">
              <span className={cn(net > 0 ? "text-success" : net < 0 ? "text-destructive" : undefined)}>
                <CountUp value={net} format={(n) => `${n > 0 ? "+" : ""}${n.toLocaleString("fr-FR")}`} />
              </span>
            </SummaryTile>
            <SummaryTile
              label="En jeu"
              hint={inPlay.length > 0 ? `jusqu'à ${inPlayPotential.toLocaleString("fr-FR")} pts à gagner` : "aucun ticket ouvert"}
            >
              <CountUp value={inPlayStake} format={(n) => `${n.toLocaleString("fr-FR")} pts`} />
            </SummaryTile>
          </div>

          <div className="glass-scroll -mx-1 flex gap-2 overflow-x-auto px-1 pb-1" role="group" aria-label="Filtrer les tickets">
            {FILTERS.map(({ value, label }) => (
              <button
                key={value}
                type="button"
                onClick={() => {
                  setFilter(value);
                  setPage(1);
                }}
                aria-pressed={filter === value}
                disabled={value !== "all" && counts[value] === 0}
                className={cn(
                  "flex shrink-0 items-center gap-1.5 rounded-full px-3 py-1.5 text-sm transition-colors disabled:opacity-40",
                  filter === value ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
                )}
              >
                {label}
                <span className="font-mono text-xs">{counts[value]}</span>
              </button>
            ))}
          </div>

          <div className="flex flex-col gap-3">
            {pageItems.map((bet) => (
              <TicketCard key={bet.id} bet={bet} matchesById={matchesById} />
            ))}
          </div>

          {filtered.length > PAGE_SIZE && (
            <PaginationControls page={page} pageCount={pageCount} onPageChange={setPage} />
          )}
        </>
      )}
    </div>
  );
}
