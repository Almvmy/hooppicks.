"use client";

import { use } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { DebatePanel } from "@/components/debate-panel";
import { NightThread } from "@/components/night-thread";
import { ShareImageButton } from "@/components/share-image-button";
import { debateDayLabel, debateShare, fetchDebate, fetchTodayDebate } from "@/lib/api/debate";

export default function DebatPage({ searchParams }: { searchParams: Promise<{ jour?: string }> }) {
  const { jour } = use(searchParams);
  const query = useQuery({
    queryKey: jour ? ["debate", jour] : ["debate", "today"],
    queryFn: () => (jour ? fetchDebate(jour) : fetchTodayDebate()),
    refetchInterval: jour ? false : 30_000,
  });
  const debate = query.data;
  const revealed = debate && debate.votesA !== null;

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-4">
      {jour && (
        <Link href="/debat" className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="h-4 w-4" />
          Le débat du jour
        </Link>
      )}
      <h1 className="font-heading text-2xl font-bold">{jour ? `Débat du ${debateDayLabel(jour)}` : "Le débat du jour"}</h1>

      {query.isLoading && <Skeleton className="h-48 w-full rounded-2xl" />}
      {query.isError && <p className="text-sm text-destructive">Impossible de charger le débat.</p>}

      {debate && (
        <>
          <Card>
            <CardContent className="flex flex-col gap-3 pt-5">
              <DebatePanel debate={debate} />
              {revealed && debate.total > 0 && (
                <ShareImageButton src={`/debat/carte?jour=${debate.day}`} fileName="hooppicks-debat.png" text={`${debate.question} Et toi ?`} label="Partager le résultat" />
              )}
            </CardContent>
          </Card>

          {revealed ? (
            <section className="flex flex-col gap-3">
              <h2 className="font-heading text-lg font-bold">Les arguments</h2>
              <NightThread debateDay={debate.day} />
            </section>
          ) : (
            <p className="text-center text-sm text-muted-foreground">Vote pour voir le résultat et les arguments.</p>
          )}

          {debate.history.length > 0 && !jour && (
            <section className="flex flex-col gap-2">
              <h2 className="font-heading text-lg font-bold">Les derniers débats</h2>
              {debate.history.map((past) => {
                const aLeads = past.votesA >= past.votesB;
                const leader = aLeads ? past.optionA : past.optionB;
                const share = aLeads ? debateShare(past.votesA, past.votesB) : debateShare(past.votesB, past.votesA);
                return (
                  <Link key={past.day} href={`/debat?jour=${past.day}`} className="glass flex items-center gap-3 rounded-2xl px-4 py-3 text-sm hover:text-primary">
                    <span className="min-w-0 flex-1">
                      <span className="block truncate font-medium">{past.question}</span>
                      <span className="text-xs text-muted-foreground">{debateDayLabel(past.day)}</span>
                    </span>
                    <span className="shrink-0 text-right">
                      <span className="block max-w-32 truncate text-xs font-semibold">{leader}</span>
                      <span className="font-mono text-sm font-bold">{share} %</span>
                    </span>
                  </Link>
                );
              })}
            </section>
          )}
        </>
      )}
    </div>
  );
}
