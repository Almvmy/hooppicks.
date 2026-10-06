import Link from "next/link";
import { Crown } from "lucide-react";
import { TeamLogo } from "@/components/team-logo";
import { Match } from "@/lib/types";
import { formatMatchDate } from "@/lib/utils";

interface Series {
  key: string;
  round: string;
  summary: string | null;
  games: Match[];
}

// "1er tour Est · Match 2" -> ["1er tour Est", "2"]
function splitStage(label: string | null): [string, string | null] {
  const m = label?.match(/^(.*) · Match (\d+)$/);
  return m ? [m[1], m[2]] : [label ?? "Playoffs", null];
}

function groupSeries(matches: Match[]): Series[] {
  const byPair = new Map<string, Match[]>();
  for (const m of matches) {
    if (m.type !== "playoffs") continue;
    // Même série quel que soit le terrain : la paire d'équipes, triée.
    const key = [m.homeTeam.abbreviation, m.awayTeam.abbreviation].sort().join("-");
    byPair.set(key, [...(byPair.get(key) ?? []), m]);
  }
  return [...byPair.entries()]
    .map(([key, games]) => {
      games.sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
      // État le plus récent connu : celui du dernier match qui en porte un.
      const withSummary = [...games].reverse().find((g) => g.seriesSummary);
      const last = games[games.length - 1];
      return { key, round: splitStage(last.stageLabel)[0], summary: withSummary?.seriesSummary ?? null, games };
    })
    .sort(
      (a, b) =>
        new Date(b.games[b.games.length - 1].date).getTime() - new Date(a.games[a.games.length - 1].date).getTime()
    );
}

/** Vue playoffs : un bloc par série (tour, état de la série, ses matchs). */
export function PlayoffSeries({ matches }: { matches: Match[] }) {
  const series = groupSeries(matches);
  if (series.length === 0) return null;

  return (
    <section className="flex flex-col gap-3">
      <h2 className="flex items-center gap-2 font-heading text-sm font-bold uppercase tracking-wide text-primary">
        <Crown className="h-4 w-4" />
        Séries
      </h2>
      <div className="stagger-children grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        {series.map(({ key, round, summary, games }) => {
          const { homeTeam, awayTeam } = games[0];
          return (
            <div key={key} className="glass flex flex-col gap-3 rounded-2xl p-4">
              <p className="text-[11px] font-bold uppercase tracking-wide text-muted-foreground">{round}</p>
              <div className="flex items-center justify-between gap-2">
                {[awayTeam, homeTeam].map((t) => (
                  <span key={t.id} className="flex items-center gap-2 font-heading font-bold">
                    <TeamLogo abbreviation={t.abbreviation} logoUrl={t.logoUrl} size={32} />
                    {t.abbreviation}
                  </span>
                ))}
              </div>
              {summary && <p className="text-center font-mono text-sm font-semibold">{summary}</p>}
              <div className="flex flex-col">
                {games.map((g) => {
                  const [, gameNumber] = splitStage(g.stageLabel);
                  const played = g.status !== "scheduled" && g.homeScore != null && g.awayScore != null;
                  return (
                    <Link
                      key={g.id}
                      href={`/matches/${g.id}`}
                      className="flex items-center justify-between rounded-lg px-1 py-1 text-xs transition-colors hover:bg-tint/5"
                    >
                      <span className="text-muted-foreground">{gameNumber ? `Match ${gameNumber}` : "Match"}</span>
                      <span className="font-mono">
                        {played
                          ? `${g.awayTeam.abbreviation} ${g.awayScore} - ${g.homeScore} ${g.homeTeam.abbreviation}`
                          : formatMatchDate(new Date(g.date))}
                      </span>
                    </Link>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
