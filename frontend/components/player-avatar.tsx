import { Flame, Target, Sparkles, Shield, Compass } from "lucide-react";
import { cn } from "@/lib/utils";
import { AvatarColorway, AvatarIcon, AvatarPosition } from "@/lib/types";

export const AVATAR_COLORWAYS: Record<AvatarColorway, { from: string; to: string; label: string }> = {
  orange: { from: "#FF7A1A", to: "#C2410C", label: "Braise" },
  purple: { from: "#A855F7", to: "#6D28D9", label: "Améthyste" },
  blue: { from: "#3B82F6", to: "#1D4ED8", label: "Azur" },
  green: { from: "#22C55E", to: "#15803D", label: "Émeraude" },
  red: { from: "#EF4444", to: "#B91C1C", label: "Grenat" },
  teal: { from: "#14B8A6", to: "#0F766E", label: "Lagon" },
};

export const AVATAR_ICONS: Record<AvatarIcon, { icon: React.ElementType; label: string }> = {
  dunk: { icon: Flame, label: "Finisseur" },
  three: { icon: Target, label: "Sniper" },
  handles: { icon: Sparkles, label: "Dribbleur" },
  defense: { icon: Shield, label: "Défenseur" },
  playmaker: { icon: Compass, label: "Meneur" },
};

export const AVATAR_POSITIONS: AvatarPosition[] = ["PG", "SG", "SF", "PF", "C"];

const SIZE_PX: Record<"sm" | "md" | "lg" | "xl", number> = {
  sm: 32,
  md: 48,
  lg: 72,
  xl: 128,
};

interface PlayerAvatarProps {
  number: number;
  position: AvatarPosition;
  colorway: AvatarColorway;
  icon: AvatarIcon;
  size?: "sm" | "md" | "lg" | "xl";
  className?: string;
}

/**
 * Avatar "carte joueur" généré entièrement en SVG à partir de choix
 * personnalisables (numéro, poste, palette, style de jeu). Aucune image
 * externe, aucun visage réel — donc aucun souci de droit à l'image ou de
 * copyright, contrairement à un catalogue de photos de joueurs NBA.
 */
export function PlayerAvatar({
  number,
  position,
  colorway,
  icon,
  size = "md",
  className,
}: PlayerAvatarProps) {
  const palette = AVATAR_COLORWAYS[colorway];
  const { icon: Icon } = AVATAR_ICONS[icon];
  const px = SIZE_PX[size];
  const gradientId = `avatar-gradient-${colorway}-${size}`;
  const showBadges = size !== "sm";

  return (
    <div
      className={cn("relative shrink-0", className)}
      style={{ width: px, height: px }}
    >
      <svg viewBox="0 0 100 100" width={px} height={px}>
        <defs>
          <linearGradient id={gradientId} x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor={palette.from} />
            <stop offset="100%" stopColor={palette.to} />
          </linearGradient>
        </defs>
        <circle cx="50" cy="50" r="48" fill={`url(#${gradientId})`} />
        <circle cx="50" cy="50" r="48" fill="none" stroke="rgba(255,255,255,0.25)" strokeWidth="2" />
        <text
          x="50"
          y="60"
          textAnchor="middle"
          fontWeight="700"
          fontSize="40"
          fill="white"
          style={{ fontFamily: "var(--font-heading)" }}
        >
          {number}
        </text>
      </svg>

      {showBadges && (
        <>
          <span
            className="absolute -bottom-1 -right-1 flex items-center justify-center rounded-full border-2 border-background bg-background/95"
            style={{ width: px * 0.42, height: px * 0.42 }}
            title={AVATAR_ICONS[icon].label}
          >
            <Icon className="h-[60%] w-[60%] text-foreground" />
          </span>
          <span className="absolute -top-1.5 left-1/2 -translate-x-1/2 rounded-full border border-background bg-background px-1.5 py-0.5 font-mono text-[9px] font-bold leading-none">
            {position}
          </span>
        </>
      )}
    </div>
  );
}
