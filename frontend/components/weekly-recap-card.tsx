import { LogoSymbol } from "@/app/LogoSymbol";

function headlineFor(streak: number, weeklyDelta: number): string {
  if (streak >= 5) return `${streak} pronostics gagnés d'affilée.\nNuit parfaite.`;
  if (streak >= 3) return `${streak} pronostics gagnés d'affilée.\nÇa chauffe.`;
  if (weeklyDelta > 0) return "Une bonne semaine.\nEncore un peu de sauce.";
  if (weeklyDelta < 0) return "Semaine compliquée.\nOn revient plus fort.";
  return "Nouvelle semaine.\nNouveaux pronostics.";
}

export function WeeklyRecapCard({
  username,
  weeklyDelta,
  rank,
  totalPlayers,
  winRate,
  streak,
  seasonLabel,
}: {
  username: string;
  weeklyDelta: number;
  rank?: number;
  totalPlayers?: number;
  winRate: number;
  streak: number;
  seasonLabel: string;
}) {
  const headline = headlineFor(streak, weeklyDelta);

  return (
    <div className="recap-sheen relative w-full max-w-[420px] overflow-hidden rounded-xl border border-border bg-gradient-to-br from-[#1A2440] to-background p-6">
      <svg
        aria-hidden
        viewBox="0 0 300 200"
        className="pointer-events-none absolute -bottom-16 -right-10 w-[280px] opacity-[0.18]"
        fill="none"
      >
        <path
          d="M80 0 C140 50 140 150 80 200 M170 -10 C230 50 230 150 170 210"
          stroke="var(--primary)"
          strokeWidth="3"
        />
      </svg>

      <div className="relative flex items-center justify-between">
        <div className="flex items-center gap-2">
          <LogoSymbol variant="compact" className="h-5 w-5 shrink-0" />
          <span className="font-heading text-sm font-bold">
            Hoop<span className="text-primary">Picks</span>
          </span>
        </div>
        <span className="font-mono text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
          {seasonLabel}
        </span>
      </div>

      <p className="relative mt-6 font-heading text-2xl font-bold leading-tight tracking-tight sm:text-3xl">
        {headline.split("\n").map((line, i) => (
          <span key={i} className={i === 1 ? "text-primary" : undefined}>
            {i === 1 && <br />}
            {line}
          </span>
        ))}
      </p>

      <div className="relative mt-6 flex items-end justify-between">
        <div className="flex gap-5">
          <div>
            <div
              className={`font-mono text-xl font-bold ${
                weeklyDelta >= 0 ? "text-success" : "text-destructive"
              }`}
            >
              {weeklyDelta > 0 ? "+" : ""}
              {weeklyDelta.toLocaleString("fr-FR")}
            </div>
            <div className="mt-1 font-mono text-[9px] font-medium tracking-wider text-muted-foreground">
              POINTS (7J)
            </div>
          </div>
          <div>
            <div className="font-mono text-xl font-bold">{rank ? `#${rank}` : "-"}</div>
            <div className="mt-1 font-mono text-[9px] font-medium tracking-wider text-muted-foreground">
              CLASSEMENT{totalPlayers ? ` / ${totalPlayers}` : ""}
            </div>
          </div>
          <div>
            <div className="font-mono text-xl font-bold">{winRate}%</div>
            <div className="mt-1 font-mono text-[9px] font-medium tracking-wider text-muted-foreground">
              RÉUSSITE
            </div>
          </div>
        </div>
        <span className="text-sm font-semibold text-muted-foreground">@{username}</span>
      </div>
    </div>
  );
}
