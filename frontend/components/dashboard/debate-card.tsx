"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { MessagesSquare } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { DebatePanel } from "@/components/debate-panel";
import { fetchTodayDebate } from "@/lib/api/debate";

/** Le débat du jour sur l'accueil : on vote sans quitter la page, les arguments sont sur /debat. */
export function DebateCard() {
  const { data, isError } = useQuery({ queryKey: ["debate", "today"], queryFn: fetchTodayDebate, staleTime: 60_000 });
  if (isError || !data) return null;

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-5">
        <div className="flex items-center justify-between gap-3">
          <h2 className="flex items-center gap-2 font-heading text-base font-bold">
            <MessagesSquare className="h-4 w-4 text-primary" />
            Débat du jour
          </h2>
          {data.myVote && (
            <Link href="/debat" className="text-xs font-semibold text-primary hover:underline">
              {data.comments > 0 ? `${data.comments} argument${data.comments > 1 ? "s" : ""}` : "Argumenter"}
            </Link>
          )}
        </div>
        <DebatePanel debate={data} />
      </CardContent>
    </Card>
  );
}
