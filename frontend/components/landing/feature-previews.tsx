import { Cloud, Crown, Flame, Medal, Target, Ticket, Trophy, Users } from "lucide-react";
import { TeamLogo } from "@/components/team-logo";
import { LeaderboardEntry, Match } from "@/lib/types";
import { cn } from "@/lib/utils";
import { MatchDate, MatchTime } from "@/components/local-date";

// Aperçus des fonctionnalités pour le carrousel d'accueil. Rendus avec les
// vrais composants et, quand elles existent, les vraies données (prochain
// match, classement) plutôt que des captures d'écran : ils restent à jour
// tout seuls à chaque évolution de l'interface.

function Odds({ label, value }: { label: string; value: number }) {
  return (
    <div className="glass-inset-quiet flex flex-1 flex-col items-center rounded-xl py-2">
      <span className="text-[10px] text-muted-foreground">{label}</span>
      <span className="font-mono text-sm font-bold">{value.toFixed(2)}</span>
    </div>
  );
}

export function MatchPreview({ match }: { match: Match | undefined }) {
  if (!match) {
    return (
      <div className="flex h-full flex-col justify-center gap-3">
        <p className="text-center text-sm text-muted-foreground">Moneyline, spread et total sur chaque affiche.</p>
        <div className="flex gap-2">
          <Odds label="Extérieur" value={2.1} />
          <Odds label="Domicile" value={1.75} />
        </div>
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-3">
      <p className="text-center font-mono text-[11px] text-muted-foreground">
        <MatchDate iso={match.date} /> · <MatchTime iso={match.date} />
      </p>
      <div className="flex items-center justify-around">
        {[match.awayTeam, match.homeTeam].map((team, i) => (
          <div key={team.id} className="flex flex-col items-center gap-1.5">
            <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={40} />
            <span className="text-xs font-semibold">{team.abbreviation}</span>
            {i === 0 && <span className="sr-only">contre</span>}
          </div>
        ))}
      </div>
      <div className="flex gap-2">
        <Odds label={`${match.awayTeam.abbreviation} (V)`} value={match.odds.moneylineAway} />
        <Odds label={`${match.homeTeam.abbreviation} (V)`} value={match.odds.moneylineHome} />
      </div>
    </div>
  );
}

const COMBO = [
  { pick: "BOS vainqueur", odds: 1.62 },
  { pick: "LAL +4.5", odds: 1.91 },
  { pick: "Plus de 224.5", odds: 1.91 },
];

export function ComboPreview() {
  const total = COMBO.reduce((acc, s) => acc * s.odds, 1);
  return (
    <div className="flex flex-col gap-2">
      {COMBO.map((s) => (
        <div key={s.pick} className="glass-inset-quiet flex items-center justify-between rounded-lg px-3 py-1.5 text-xs">
          <span>{s.pick}</span>
          <span className="font-mono font-bold">{s.odds.toFixed(2)}</span>
        </div>
      ))}
      <div className="mt-1 flex items-center justify-between px-1 text-xs">
        <span className="text-muted-foreground">Cote totale</span>
        <span className="font-mono font-bold text-primary">× {total.toFixed(2)}</span>
      </div>
      <div className="flex items-center justify-between px-1 text-xs">
        <span className="text-muted-foreground">Mise 50 pts</span>
        <span className="font-mono font-bold">gain {Math.round(50 * total)} pts</span>
      </div>
    </div>
  );
}

export function LeaderboardSlidePreview({ entries }: { entries: LeaderboardEntry[] }) {
  const rows = entries.slice(0, 4);
  if (rows.length === 0) {
    return (
      <div className="flex h-full flex-col items-center justify-center gap-2 text-center">
        <Trophy className="h-10 w-10 text-primary" />
        <p className="text-sm text-muted-foreground">La première place est encore libre.</p>
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-1.5">
      {rows.map((entry) => (
        <div
          key={entry.username}
          className={cn(
            "flex items-center justify-between rounded-lg px-3 py-1.5 text-xs",
            entry.rank === 1 ? "glass-accent" : "glass-inset-quiet"
          )}
        >
          <span className="flex min-w-0 items-center gap-2">
            <span className={cn("font-mono font-bold", entry.rank === 1 && "text-primary")}>#{entry.rank}</span>
            <span className="truncate">{entry.username}</span>
          </span>
          <span className="font-mono font-bold">{entry.points.toLocaleString("fr-FR")}</span>
        </div>
      ))}
    </div>
  );
}

export function LeaguePreview() {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-3">
      <div className="flex -space-x-2">
        {["bg-orange-500", "bg-sky-500", "bg-emerald-500", "bg-violet-500"].map((color, i) => (
          <span
            key={color}
            className={cn("flex h-9 w-9 items-center justify-center rounded-full border-2 border-background text-xs font-bold text-white", color)}
          >
            {"ABCD"[i]}
          </span>
        ))}
      </div>
      <div className="glass-inset-quiet flex items-center gap-2 rounded-lg px-3 py-1.5">
        <Users className="h-3.5 w-3.5 text-muted-foreground" />
        <span className="text-[10px] uppercase tracking-widest text-muted-foreground">Code</span>
        <span className="font-mono text-sm font-bold tracking-[0.3em]">HP7K2Q</span>
      </div>
    </div>
  );
}

// Vrais badges de l'app (cf. BadgeService) : même nom, même icône.
const BADGES = [
  { icon: Ticket, label: "Premier ticket" },
  { icon: Flame, label: "Main chaude" },
  { icon: Target, label: "Sniper" },
  { icon: Crown, label: "Roi du multiple" },
  { icon: Cloud, label: "Sur un nuage" },
  { icon: Medal, label: "Vétéran" },
];

export function BadgesPreview() {
  return (
    <div className="grid grid-cols-3 gap-2">
      {BADGES.map(({ icon: Icon, label }, i) => (
        <div
          key={label}
          className={cn(
            "flex flex-col items-center gap-1 rounded-xl py-2",
            i < 4 ? "glass-accent" : "glass-inset-quiet opacity-50"
          )}
        >
          <Icon className={cn("h-5 w-5", i < 4 ? "text-primary" : "text-muted-foreground")} />
          <span className="text-center text-[10px] leading-tight">{label}</span>
        </div>
      ))}
    </div>
  );
}

const STAT_LINE = [
  { label: "PTS", value: 31 },
  { label: "REB", value: 8 },
  { label: "AST", value: 11 },
  { label: "MIN", value: 36 },
];

export function PlayerStatsPreview() {
  return (
    <div className="flex h-full flex-col justify-center gap-3">
      <div className="flex items-center gap-3">
        <span className="flex h-11 w-11 items-center justify-center rounded-full bg-primary/15 font-mono text-sm font-bold text-primary">
          #7
        </span>
        <div>
          <p className="text-sm font-semibold">Feuille de match</p>
          <p className="text-[11px] text-muted-foreground">Effectifs, blessures, moyennes</p>
        </div>
      </div>
      <div className="grid grid-cols-4 gap-1.5">
        {STAT_LINE.map((s) => (
          <div key={s.label} className="glass-inset-quiet flex flex-col items-center rounded-lg py-1.5">
            <span className="font-mono text-sm font-bold">{s.value}</span>
            <span className="text-[9px] text-muted-foreground">{s.label}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
