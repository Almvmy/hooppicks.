"use client";

import { useState, type ReactNode } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Check, ChevronDown, ChevronRight } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { MissionsDetail } from "@/components/dashboard/missions-card";
import { fetchBracket } from "@/lib/api/bracket";
import { fetchTodayDebate } from "@/lib/api/debate";
import { fetchMissions } from "@/lib/api/missions";
import { fetchQuiz, shotsCount, shotsLine } from "@/lib/api/quiz";
import { fetchSeasonPicks } from "@/lib/api/season-picks";
import type { Match, PlacedBet } from "@/lib/types";
import { cn, formatMatchDate } from "@/lib/utils";

const DAY = 24 * 3600 * 1000;

/** Début de la soirée NBA en cours : 12h GMT, comme le quiz et le débat. */
function nightStart(now: number): number {
  const d = new Date(now);
  const noon = Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate(), 12);
  return now < noon ? noon - DAY : noon;
}

interface Task {
  key: string;
  title: string;
  detail: ReactNode;
  done: boolean;
  action: string;
  href: string;
}

function Tick({ done }: { done: boolean }) {
  return (
    <span
      className={cn("flex h-6 w-6 shrink-0 items-center justify-center rounded-full", done ? "bg-success text-background" : "border-2 border-tint/25")}
      aria-label={done ? "Fait" : "À faire"}
    >
      {done && <Check className="h-4 w-4" />}
    </span>
  );
}

/**
 * Ce qu'il reste à faire aujourd'hui, en une carte : remplace une carte par
 * rendez-vous (quiz, débat, missions, tableau, pronostics), qui allongeaient
 * l'accueil et ne disaient pas ce qui était déjà fait. La première tâche restante
 * ressort : c'est elle qui fait revenir chaque jour.
 */
export function TodayCard({ bets, matches }: { bets: PlacedBet[] | undefined; matches: Match[] | undefined }) {
  const [now] = useState(() => Date.now());
  const [missionsOpen, setMissionsOpen] = useState(false);
  const quiz = useQuery({ queryKey: ["quiz", "state"], queryFn: fetchQuiz, staleTime: 60_000 });
  const debate = useQuery({ queryKey: ["debate", "today"], queryFn: fetchTodayDebate, staleTime: 60_000 });
  const missions = useQuery({ queryKey: ["missions"], queryFn: fetchMissions, staleTime: 60_000 });
  const bracket = useQuery({ queryKey: ["bracket"], queryFn: fetchBracket, staleTime: 5 * 60 * 1000 });
  const picks = useQuery({ queryKey: ["season-picks"], queryFn: fetchSeasonPicks, staleTime: 5 * 60 * 1000 });

  const start = nightStart(now);
  const ticketTonight = (bets ?? []).some((b) => new Date(b.placedAt).getTime() >= start);
  const gamesTonight = (matches ?? []).some((m) => {
    const t = new Date(m.date).getTime();
    return m.status === "scheduled" && t >= now && t < start + DAY;
  });

  const tasks: Task[] = [];
  const quizDone = quiz.data?.status === "done";
  // Pas de condition sur la réponse du serveur : le lien, lui, n'a besoin de rien.
  tasks.push({
    key: "quiz",
    title: "Quiz du jour",
    detail: quizDone ? `${shotsLine(quiz.data!.shots)} ${shotsCount(quiz.data!.shots)}/5` : "5 tirs, 15 secondes par tir",
    done: quizDone,
    action: quiz.data?.status === "playing" ? "Reprendre" : "Jouer",
    href: "/quiz",
  });
  if (gamesTonight || ticketTonight) {
    tasks.push({
      key: "ticket",
      title: "Ton ticket du soir",
      detail: ticketTonight ? "Ticket posé" : "Les matchs de ce soir t'attendent",
      done: ticketTonight,
      action: "Parier",
      href: "/matches",
    });
  }
  const myVote = debate.data?.myVote;
  tasks.push({
    key: "debat",
    title: "Débat du jour",
    detail: debate.data ? (myVote ? `Ton camp : ${myVote === "A" ? debate.data.optionA : debate.data.optionB}` : debate.data.question) : "Choisis ton camp",
    done: !!myVote,
    action: "Voter",
    href: "/debat",
  });
  if (bracket.data && !bracket.data.locked) {
    tasks.push({
      key: "tableau",
      title: bracket.data.label,
      detail: bracket.data.myPicks ? "Tableau rempli" : `Ton tableau, avant le ${formatMatchDate(new Date(bracket.data.locksAt))}`,
      done: !!bracket.data.myPicks,
      action: "Remplir",
      href: "/coupe",
    });
  }
  if (picks.data && !picks.data.locked && Object.keys(picks.data.myPicks).length < picks.data.questions.length) {
    tasks.push({
      key: "saison",
      title: "Pronostics de saison",
      detail: `${Object.keys(picks.data.myPicks).length}/${picks.data.questions.length}${picks.data.deadline ? ` · avant le ${formatMatchDate(new Date(picks.data.deadline))}` : ""}`,
      done: false,
      action: "Choisir",
      href: "/saison",
    });
  }

  const missionsDone = missions.data ? missions.data.missions.filter((m) => m.done).length : 0;
  const missionsTotal = missions.data?.missions.length ?? 0;
  const allMissions = missionsTotal > 0 && missionsDone === missionsTotal;

  const doneCount = tasks.filter((t) => t.done).length;
  const nextKey = tasks.find((t) => !t.done)?.key;

  return (
    <Card>
      <CardContent className="flex flex-col gap-1 pt-5">
        <div className="mb-1 flex items-center justify-between gap-3">
          <h2 className="font-heading text-lg font-bold">À faire aujourd&apos;hui</h2>
          <span className="font-mono text-sm font-bold">
            {doneCount}/{tasks.length}
          </span>
        </div>

        {tasks.map((task) => {
          const next = task.key === nextKey;
          return (
            <Link key={task.key} href={task.href} className="-mx-2 flex items-center gap-3 rounded-xl px-2 py-2.5 transition-colors hover:bg-tint/[0.04]">
              <Tick done={task.done} />
              <span className="min-w-0 flex-1">
                <span className={cn("block text-sm font-semibold", task.done && "text-muted-foreground")}>{task.title}</span>
                <span className="block truncate text-xs text-muted-foreground">{task.detail}</span>
              </span>
              {task.done ? (
                <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" />
              ) : (
                <span className={cn("shrink-0 rounded-full px-3.5 py-1.5 text-xs font-bold", next ? "bg-[#FF7A1A] text-[#0B1120]" : "glass-inset-quiet text-foreground")}>{task.action}</span>
              )}
            </Link>
          );
        })}

        {missions.data && missionsTotal > 0 && (
          <div className="-mx-2 border-t border-tint/10 pt-1">
            <button
              type="button"
              aria-expanded={missionsOpen}
              onClick={() => setMissionsOpen((v) => !v)}
              className="flex w-full items-center gap-3 rounded-xl px-2 py-2.5 text-left transition-colors hover:bg-tint/[0.04]"
            >
              <Tick done={allMissions} />
              <span className="min-w-0 flex-1">
                <span className={cn("block text-sm font-semibold", allMissions && "text-muted-foreground")}>Missions de la semaine</span>
                <span className="block text-xs text-muted-foreground">
                  {missionsDone}/{missionsTotal} réussies
                </span>
              </span>
              <ChevronDown className={cn("h-4 w-4 shrink-0 text-muted-foreground transition-transform", missionsOpen && "rotate-180")} />
            </button>
            {missionsOpen && (
              <div className="px-2 pb-2 pt-1">
                <MissionsDetail data={missions.data} />
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
