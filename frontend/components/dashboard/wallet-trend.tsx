import { TrendingUp, TrendingDown, Minus } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

const WIDTH = 280;
const HEIGHT = 64;

function buildPath(points: number[]): { line: string; area: string } {
  if (points.length < 2) return { line: "", area: "" };

  const min = Math.min(...points);
  const max = Math.max(...points);
  const range = max - min || 1;
  const step = WIDTH / (points.length - 1);

  const coords = points.map((p, i) => {
    const x = i * step;
    const y = HEIGHT - ((p - min) / range) * (HEIGHT - 8) - 4;
    return [x, y];
  });

  const line = coords.map(([x, y], i) => `${i === 0 ? "M" : "L"}${x.toFixed(1)},${y.toFixed(1)}`).join(" ");
  const area = `${line} L${WIDTH},${HEIGHT} L0,${HEIGHT} Z`;

  return { line, area };
}

export function WalletTrend({
  series,
  weeklyDelta,
  staked,
  won,
  isLoading,
}: {
  series: number[];
  weeklyDelta: number;
  staked: number;
  won: number;
  isLoading: boolean;
}) {
  const { line, area } = buildPath(series);
  const isUp = weeklyDelta > 0;
  const isFlat = weeklyDelta === 0;

  return (
    <Card>
      <CardHeader className="space-y-0">
        <CardTitle className="font-heading text-base">Solde : 7 derniers jours</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : (
          <>
            <div className="flex items-center justify-between">
              <div
                className={cn(
                  "flex items-center gap-1 text-sm font-semibold",
                  isFlat
                    ? "text-muted-foreground"
                    : isUp
                    ? "text-success"
                    : "text-destructive"
                )}
              >
                {isFlat ? (
                  <Minus className="h-3.5 w-3.5" />
                ) : isUp ? (
                  <TrendingUp className="h-3.5 w-3.5" />
                ) : (
                  <TrendingDown className="h-3.5 w-3.5" />
                )}
                {weeklyDelta > 0 ? "+" : ""}
                {weeklyDelta.toLocaleString("fr-FR")} pts
              </div>
            </div>

            <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} className="w-full" preserveAspectRatio="none">
              <defs>
                <linearGradient id="walletTrendFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="var(--primary)" stopOpacity="0.35" />
                  <stop offset="100%" stopColor="var(--primary)" stopOpacity="0" />
                </linearGradient>
              </defs>
              {area && <path d={area} fill="url(#walletTrendFill)" />}
              {line && (
                <path d={line} fill="none" stroke="var(--primary)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
              )}
            </svg>

            <div className="glass-hairline-t flex items-center justify-between pt-3 text-xs">
              <span className="text-muted-foreground">
                Misé <span className="font-mono text-foreground">{staked.toLocaleString("fr-FR")}</span> pts
              </span>
              <span className="text-muted-foreground">
                Gagné <span className="font-mono text-foreground">{won.toLocaleString("fr-FR")}</span> pts
              </span>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
}
