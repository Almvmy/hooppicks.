import { Card, CardContent } from "@/components/ui/card";
import { netResult } from "@/lib/bet-legs";
import type { PlacedBet } from "@/lib/types";
import { cn, formatSignedPoints, nextBankrollReset } from "@/lib/utils";

const WEEK_MS = 7 * 24 * 3600 * 1000;

/**
 * Bénéfice de chaque semaine de jeu (lundi 12h GMT), d'après les tickets
 * réglés, comptés à la semaine où ils ont été posés, comme le classement. Une
 * semaine négative s'affiche en rouge : au classement, elle compte pour 0.
 */
export function weeklyNet(bets: PlacedBet[] | undefined): { start: number; net: number }[] {
  const weeks = new Map<number, number>();
  for (const b of bets ?? []) {
    if (b.status !== "won" && b.status !== "lost") continue;
    const start = nextBankrollReset(new Date(b.placedAt)).getTime() - WEEK_MS;
    weeks.set(start, (weeks.get(start) ?? 0) + netResult(b));
  }
  return [...weeks.entries()].sort((a, b) => a[0] - b[0]).map(([start, net]) => ({ start, net }));
}

const dayMonth = (t: number) => new Date(t).toLocaleDateString("fr-FR", { day: "numeric", month: "short", timeZone: "UTC" });

export function SeasonWeeks({ bets }: { bets: PlacedBet[] | undefined }) {
  const weeks = weeklyNet(bets).slice(-12);
  if (weeks.length === 0) return null;
  const max = Math.max(1, ...weeks.map((w) => Math.abs(w.net)));
  const best = weeks.reduce((a, b) => (b.net > a.net ? b : a));

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-6">
        <div className="flex items-baseline justify-between gap-3">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Tes semaines</p>
          {best.net > 0 && (
            <p className="text-xs text-muted-foreground">
              Meilleure : <span className="font-mono font-bold text-foreground">{formatSignedPoints(best.net)}</span>
            </p>
          )}
        </div>
        {/* Barres au-dessus (gain) ou en dessous (perte) d'une ligne de zéro. */}
        <div className="flex h-32 items-stretch justify-center gap-1.5" role="img" aria-label="Bénéfice de chaque semaine de jeu">
          {weeks.map((w) => {
            const h = Math.max(4, (Math.abs(w.net) / max) * 50);
            return (
              <div key={w.start} className="group relative flex max-w-8 flex-1 flex-col" title={`Semaine du ${dayMonth(w.start)} : ${formatSignedPoints(w.net)}`}>
                <div className="flex flex-1 items-end">
                  {w.net >= 0 && <div className="w-full rounded-t-md bg-success/80" style={{ height: `${h * 2}%` }} />}
                </div>
                <div className="h-px bg-tint/20" />
                <div className="flex flex-1 items-start">
                  {w.net < 0 && <div className="w-full rounded-b-md bg-destructive/70" style={{ height: `${h * 2}%` }} />}
                </div>
              </div>
            );
          })}
        </div>
        <div className="flex justify-between text-[11px] text-muted-foreground">
          <span>{dayMonth(weeks[0].start)}</span>
          <span>
            Dernière :{" "}
            <span className={cn("font-mono font-bold", weeks[weeks.length - 1].net >= 0 ? "text-success" : "text-destructive")}>
              {formatSignedPoints(weeks[weeks.length - 1].net)}
            </span>
          </span>
        </div>
      </CardContent>
    </Card>
  );
}
