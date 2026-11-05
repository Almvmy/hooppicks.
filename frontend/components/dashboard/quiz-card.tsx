"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ChevronRight } from "lucide-react";
import { fetchQuiz, shotsCount, shotsLine } from "@/lib/api/quiz";

/**
 * Le quiz du jour sur l'accueil : toute la carte est le lien. Elle reste affichée
 * si le serveur ne répond pas : le lien, lui, n'a besoin de rien.
 */
export function QuizCard() {
  const { data } = useQuery({ queryKey: ["quiz", "state"], queryFn: fetchQuiz, staleTime: 60_000 });
  const status = data?.status ?? "ready";
  const standing = data?.standing;
  const done = status === "done";

  return (
    <Link
      href="/quiz"
      className="island relative block overflow-hidden rounded-2xl bg-[#101828] p-4 text-foreground shadow-[inset_0_0_0_1px_rgba(255,122,26,0.35)] transition-transform hover:-translate-y-0.5"
    >
      <span aria-hidden className="absolute inset-0" style={{ background: "radial-gradient(90% 140% at 100% 0%, rgba(255,122,26,0.28), transparent 60%)" }} />
      <span className="relative flex items-center gap-3">
        <span className="min-w-0 flex-1">
          <span className="block text-[11px] font-bold uppercase tracking-[.16em] text-[#FF7A1A]">Quiz du jour</span>
          <span className="mt-1 block font-heading text-xl font-extrabold leading-tight">
            {done ? `${shotsCount(data!.shots)}/5 panier${shotsCount(data!.shots) > 1 ? "s" : ""}` : "5 tirs, 15 secondes par tir"}
          </span>
          <span className="mt-1.5 block text-lg leading-none tracking-wide">{done ? shotsLine(data!.shots) : "🏀🏀🏀🏀🏀"}</span>
          {standing && standing.rank > 0 && (
            <span className="mt-1.5 block text-xs text-muted-foreground">
              {standing.rank === 1 ? "1er" : `${standing.rank}e`} de la semaine · {standing.baskets} panier{standing.baskets > 1 ? "s" : ""} · {standing.level}
            </span>
          )}
        </span>
        <span className="flex shrink-0 items-center gap-1 rounded-full bg-[#FF7A1A] px-4 py-2.5 text-sm font-bold text-[#0B1120]">
          {status === "ready" ? "Jouer" : status === "playing" ? "Reprendre" : "Classement"}
          <ChevronRight className="h-4 w-4" />
        </span>
      </span>
    </Link>
  );
}
