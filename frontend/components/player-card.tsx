import { cn } from "@/lib/utils";
import { AvatarColorway, AvatarIcon, AvatarPosition } from "@/lib/types";
import { AVATAR_COLORWAYS, AVATAR_ICONS, AVATAR_POSITIONS } from "@/components/player-avatar";

interface PlayerCardProps {
  username: string;
  number: number;
  position: AvatarPosition;
  colorway: AvatarColorway;
  icon: AvatarIcon;
  className?: string;
}

/**
 * "Vraie" carte de joueur : format portrait façon carte à collectionner,
 * contrairement à PlayerAvatar (le cercle compact utilisé dans les listes
 * et la barre du haut). Même filet de sécurité sur les valeurs (voir
 * PlayerAvatar) puisqu'elle affiche les mêmes données personnalisables.
 */
export function PlayerCard({ username, number, position, colorway, icon, className }: PlayerCardProps) {
  const palette = AVATAR_COLORWAYS[colorway] ?? AVATAR_COLORWAYS.orange;
  const iconEntry = AVATAR_ICONS[icon] ?? AVATAR_ICONS.dunk;
  const Icon = iconEntry.icon;
  const safePosition = AVATAR_POSITIONS.includes(position) ? position : "PG";
  const safeNumber = Number.isFinite(number) ? Math.max(0, Math.min(99, number)) : 0;

  return (
    <div
      className={cn(
        "badge-holo relative aspect-[3/4] w-40 shrink-0 overflow-hidden rounded-xl border border-white/15 shadow-lg shadow-black/30",
        className
      )}
      style={{
        background: `linear-gradient(160deg, ${palette.from} 0%, ${palette.to} 60%, #0B1120 140%)`,
      }}
    >
      {/* Grand numéro en filigrane, purement décoratif */}
      <span
        aria-hidden
        className="pointer-events-none absolute -bottom-6 -right-3 select-none font-heading text-[130px] font-black leading-none text-white/10"
      >
        {safeNumber}
      </span>

      <div className="relative flex items-center justify-between p-3">
        <span className="rounded-full bg-black/30 px-2 py-0.5 font-mono text-[10px] font-bold tracking-wide text-white">
          {safePosition}
        </span>
        <span className="flex h-6 w-6 items-center justify-center rounded-full bg-black/30" title={iconEntry.label}>
          <Icon className="h-3.5 w-3.5 text-white" />
        </span>
      </div>

      <div className="absolute inset-x-0 bottom-0 flex flex-col gap-0.5 bg-gradient-to-t from-black/70 via-black/20 to-transparent p-3 pt-8">
        <span className="font-mono text-4xl font-black leading-none text-white">
          #{safeNumber}
        </span>
        <span className="truncate text-xs font-bold uppercase tracking-wide text-white">
          {username}
        </span>
        <span className="text-[10px] text-white/70">{iconEntry.label}</span>
      </div>
    </div>
  );
}
