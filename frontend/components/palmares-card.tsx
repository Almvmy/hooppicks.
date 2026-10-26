import { Crown, Shield, Swords, Trophy } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { ChampionPhase, Palmares } from "@/lib/types";
import { monthLabel } from "@/lib/utils";

// Du plus prestigieux au moins prestigieux : la saison, les playoffs, la Coupe, le mois.
const PHASE_ORDER: Record<ChampionPhase, number> = { general: 0, playoffs: 1, cup: 2, month: 3 };

function phaseTitleLabel(t: Palmares["phaseTitles"][number]): string {
  switch (t.phase) {
    case "general":
      return `Champion HoopPicks ${t.season}`;
    case "playoffs":
      return `Champion des playoffs ${t.season}`;
    case "cup":
      return `Champion de la Coupe NBA ${t.season}`;
    case "month":
      return `Champion du mois · ${monthLabel(t.season)}`;
  }
}

/**
 * Trophées gardés à vie, du plus prestigieux au moins prestigieux : champion
 * HoopPicks de la saison, des playoffs, de la Coupe NBA, du mois, de la
 * semaine (tout HoopPicks et par ligue), puis duels gagnés.
 */
export function PalmaresCard({ palmares }: { palmares: Palmares | undefined }) {
  if (!palmares) return null;
  const phaseTitles = [...(palmares.phaseTitles ?? [])].sort(
    (a, b) =>
      Number(!!a.leagueName) - Number(!!b.leagueName) ||
      PHASE_ORDER[a.phase] - PHASE_ORDER[b.phase] ||
      b.season.localeCompare(a.season)
  );
  const empty = phaseTitles.length === 0 && palmares.weeklyTitles === 0 && palmares.leagueTitles.length === 0 && palmares.duelWins === 0;

  return (
    <Card>
      <CardContent className="flex flex-col gap-3">
        <h2 className="flex items-center gap-2 font-heading text-base font-bold">
          <Trophy className="h-4 w-4 text-primary" />
          Palmarès
        </h2>
        {empty ? (
          <p className="text-sm text-muted-foreground">Pas encore de trophée.</p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {phaseTitles.map((t) => (
              <Trophy3
                key={`${t.phase}-${t.season}-${t.leagueName ?? ""}`}
                icon={t.leagueName ? Shield : t.phase === "general" ? Trophy : Crown}
                label={`${phaseTitleLabel(t)}${t.leagueName ? ` · ${t.leagueName}` : ""}`}
                accent={!t.leagueName}
              />
            ))}
            {palmares.weeklyTitles > 0 && (
              <Trophy3 icon={Crown} count={palmares.weeklyTitles} label="Champion de la semaine" accent />
            )}
            {palmares.leagueTitles.map((l) => (
              <Trophy3 key={l.leagueName} icon={Shield} count={l.count} label={`Champion · ${l.leagueName}`} />
            ))}
            {palmares.duelWins > 0 && (
              <Trophy3 icon={Swords} count={palmares.duelWins} label={`Duel${palmares.duelWins > 1 ? "s" : ""} gagné${palmares.duelWins > 1 ? "s" : ""}`} />
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function Trophy3({
  icon: Icon,
  count,
  label,
  accent,
}: {
  icon: React.ElementType;
  count?: number;
  label: string;
  accent?: boolean;
}) {
  return (
    <div className={accent ? "badge-holo glass-accent flex items-center gap-2 rounded-xl px-3 py-2" : "glass-inset flex items-center gap-2 rounded-xl px-3 py-2"}>
      <Icon className="h-4 w-4 shrink-0 text-primary" />
      {count !== undefined && <span className="font-mono text-sm font-bold">×{count}</span>}
      <span className="text-sm">{label}</span>
    </div>
  );
}
