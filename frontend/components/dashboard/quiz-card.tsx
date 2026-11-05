"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { CircleHelp } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { fetchQuiz, shotsCount, shotsLine } from "@/lib/api/quiz";

/** Le quiz du jour sur l'accueil : l'invitation à jouer, puis le résultat et la place de la semaine. */
export function QuizCard() {
  const { data, isError } = useQuery({ queryKey: ["quiz", "state"], queryFn: fetchQuiz, staleTime: 60_000 });
  if (isError || !data) return null;
  const { standing } = data;

  return (
    <Card>
      <CardContent className="flex items-center justify-between gap-3 pt-5">
        <div className="min-w-0">
          <h2 className="flex items-center gap-2 font-heading text-base font-bold">
            <CircleHelp className="h-4 w-4 text-primary" />
            Quiz du jour
          </h2>
          {data.status === "done" ? (
            <p className="mt-1 text-sm">
              {shotsLine(data.shots)} <span className="font-mono font-bold">{shotsCount(data.shots)}/5</span>
            </p>
          ) : (
            <p className="mt-1 text-sm text-muted-foreground">5 tirs, 15 secondes par tir</p>
          )}
          {standing.rank > 0 && (
            <p className="mt-0.5 text-xs text-muted-foreground">
              {standing.rank === 1 ? "1er" : `${standing.rank}e`} de la semaine · {standing.baskets} panier{standing.baskets > 1 ? "s" : ""}
            </p>
          )}
        </div>
        <Link href="/quiz" className="glass-accent shrink-0 rounded-full px-4 py-2 text-sm font-semibold">
          {data.status === "ready" ? "Jouer" : data.status === "playing" ? "Reprendre" : "Classement"}
        </Link>
      </CardContent>
    </Card>
  );
}
