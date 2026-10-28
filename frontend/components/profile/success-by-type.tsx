import { Card, CardContent } from "@/components/ui/card";
import type { PlacedBet } from "@/lib/types";

const TYPES = [
  { key: "moneyline", label: "Vainqueur" },
  { key: "spread", label: "Écart" },
  { key: "total", label: "Total de points" },
  { key: "props", label: "Paris joueurs" },
  { key: "milestones", label: "Paliers (30+…)" },
  { key: "parlay", label: "Combinés" },
] as const;

type TypeKey = (typeof TYPES)[number]["key"];

/**
 * Type d'un ticket : un combiné à part, sinon le marché de sa sélection. Un
 * palier se reconnaît à son libellé (« 30+ pts »), construit par le serveur.
 */
function typeOf(bet: PlacedBet): TypeKey {
  if (bet.selections.length > 1) return "parlay";
  const s = bet.selections[0];
  if (s.market === "moneyline") return "moneyline";
  if (s.market === "spread" || s.market === "alt_spread") return "spread";
  if (s.market === "total" || s.market === "alt_total") return "total";
  return /\d\+ /.test(s.label) ? "milestones" : "props";
}

/** Où l'on gagne le plus : taux de réussite par type de pari, tickets réglés seulement. */
export function SuccessByType({ bets }: { bets: PlacedBet[] | undefined }) {
  const counts = new Map<TypeKey, { won: number; total: number }>();
  for (const b of bets ?? []) {
    if ((b.status !== "won" && b.status !== "lost") || b.selections.length === 0) continue;
    const k = typeOf(b);
    const c = counts.get(k) ?? { won: 0, total: 0 };
    c.total++;
    if (b.status === "won") c.won++;
    counts.set(k, c);
  }
  const rows = TYPES.filter((t) => counts.has(t.key)).map((t) => ({ ...t, ...counts.get(t.key)! }));
  if (rows.length === 0) return null;
  const best = rows.filter((r) => r.total >= 3).reduce<(typeof rows)[number] | null>((a, r) => (!a || r.won / r.total > a.won / a.total ? r : a), null);

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-6">
        <div className="flex items-baseline justify-between gap-3">
          <p className="text-xs uppercase tracking-wide text-muted-foreground">Réussite par type de pari</p>
          {best && <p className="text-xs text-muted-foreground">Ton point fort : <span className="font-semibold text-foreground">{best.label}</span></p>}
        </div>
        <div className="flex flex-col gap-2">
          {rows.map((r) => {
            const pct = Math.round((r.won / r.total) * 100);
            return (
              <div key={r.key} className="flex items-center gap-3 text-sm">
                <span className="w-32 shrink-0 truncate">{r.label}</span>
                <div className="h-2 flex-1 overflow-hidden rounded-full bg-tint/10">
                  <div className="h-full rounded-full bg-primary" style={{ width: `${Math.max(2, pct)}%` }} />
                </div>
                <span className="w-24 shrink-0 text-right font-mono text-xs">
                  <span className="font-bold">{pct} %</span>
                  <span className="text-muted-foreground"> · {r.won}/{r.total}</span>
                </span>
              </div>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
}
