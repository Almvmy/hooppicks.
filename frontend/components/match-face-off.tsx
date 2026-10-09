import { MorphingTeamLogo, TeamLogo, teamLogoMorphName } from "@/components/team-logo";
import { getTeamColor } from "@/lib/team-colors";
import { Match, Team } from "@/lib/types";
import { cn } from "@/lib/utils";

// Briques communes de la présentation « face-à-face » des matchs : utilisées
// par la carte de match, le prochain coup d'envoi du tableau de bord, la
// page d'un match et l'accueil public, pour que toutes se ressemblent.

function tint(color: string, pct: number) {
  return `color-mix(in srgb, ${color} ${pct}%, transparent)`;
}

/**
 * Fond d'une carte de match : un léger fondu aux couleurs des deux équipes
 * (extérieur à gauche, domicile à droite), posé par-dessus le verre. À
 * passer en style inline : il remplace le background-image de .glass.
 */
export function faceOffBackground(match: Match, strength = 14): React.CSSProperties {
  const away = getTeamColor(match.awayTeam.abbreviation);
  const home = getTeamColor(match.homeTeam.abbreviation);
  return {
    backgroundImage: `linear-gradient(100deg, ${tint(away, strength)} 0%, transparent 42%, transparent 58%, ${tint(home, strength)} 100%), var(--glass-tint)`,
  };
}

function Watermark({ team, side, size, opacity }: { team: Team; side: "left" | "right"; size: number; opacity: number }) {
  return (
    <div
      className="absolute top-1/2"
      style={{
        [side]: -size * 0.28,
        opacity,
        transform: `translateY(-50%) rotate(${side === "left" ? -12 : 12}deg)`,
      }}
    >
      <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={size} />
    </div>
  );
}

/**
 * Les deux logos en filigrane géant, inclinés et coupés par les bords. Le
 * parent doit être `relative overflow-hidden`, et son contenu `relative`
 * pour passer devant.
 */
export function TeamWatermarks({ match, size = 190, opacity = 0.1 }: { match: Match; size?: number; opacity?: number }) {
  return (
    <div aria-hidden className="decor-clip">
      <Watermark team={match.awayTeam} side="left" size={size} opacity={opacity} />
      <Watermark team={match.homeTeam} side="right" size={size} opacity={opacity} />
    </div>
  );
}

function FaceOffTeam({
  match,
  side,
  logoSize,
  morph,
  nameClassName,
}: {
  match: Match;
  side: "away" | "home";
  logoSize: number;
  morph: boolean;
  nameClassName?: string;
}) {
  const team = side === "away" ? match.awayTeam : match.homeTeam;
  const logoProps = { abbreviation: team.abbreviation, logoUrl: team.logoUrl, size: logoSize };
  return (
    <div className="flex min-w-0 flex-col items-center gap-2 text-center">
      {morph ? (
        <MorphingTeamLogo morphName={teamLogoMorphName(match.id, side)} {...logoProps} />
      ) : (
        <TeamLogo {...logoProps} />
      )}
      <span className={cn("max-w-full truncate font-heading text-base font-bold leading-tight", nameClassName)}>
        {team.name}
      </span>
      {/* Discret (gris, sans icône d'alerte) et masqué en présaison : le
          rapport de blessures n'est pas encore remis à jour pour la nouvelle
          saison, l'alerte orange donnait trop de poids à une info douteuse. */}
      {!!team.outPlayersCount && match.type !== "preseason" && (
        <span
          className="text-[10px] text-muted-foreground"
          title={`${team.outPlayersCount} joueur(s) indisponible(s) (Out)`}
        >
          {team.outPlayersCount} absent{team.outPlayersCount > 1 ? "s" : ""}
        </span>
      )}
    </div>
  );
}

/**
 * Extérieur | VS ou score | Domicile. `morph` : le logo « vole » vers la page
 * du match (un seul face-à-face morphable par match et par page).
 */
export function FaceOffTeams({
  match,
  logoSize = 56,
  morph = false,
  nameClassName,
  scoreClassName,
}: {
  match: Match;
  logoSize?: number;
  morph?: boolean;
  nameClassName?: string;
  scoreClassName?: string;
}) {
  const hasScore = match.status !== "scheduled";
  const awayLeads = hasScore && (match.awayScore ?? 0) > (match.homeScore ?? 0);
  const homeLeads = hasScore && (match.homeScore ?? 0) > (match.awayScore ?? 0);

  return (
    <div className="grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-3">
      <FaceOffTeam match={match} side="away" logoSize={logoSize} morph={morph} nameClassName={nameClassName} />
      {hasScore ? (
        <span className={cn("whitespace-nowrap font-mono text-2xl font-bold tabular-nums", scoreClassName)}>
          <span className={cn(awayLeads && "text-[var(--primary-num)]")}>{match.awayScore}</span>
          <span className="mx-1.5 text-muted-foreground">–</span>
          <span className={cn(homeLeads && "text-[var(--primary-num)]")}>{match.homeScore}</span>
        </span>
      ) : (
        <span className="font-heading text-sm font-bold text-muted-foreground">VS</span>
      )}
      <FaceOffTeam match={match} side="home" logoSize={logoSize} morph={morph} nameClassName={nameClassName} />
    </div>
  );
}
