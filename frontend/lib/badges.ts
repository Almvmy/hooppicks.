import { Award, Cloud, Coins, Crown, Flame, Medal, Repeat, Target, Ticket, Zap } from "lucide-react";

export const BADGE_ICONS: Record<string, React.ElementType> = {
  ticket: Ticket,
  repeat: Repeat,
  medal: Medal,
  flame: Flame,
  cloud: Cloud,
  target: Target,
  crown: Crown,
  zap: Zap,
  coins: Coins,
};

export function badgeIcon(icon: string): React.ElementType {
  return BADGE_ICONS[icon] ?? Award;
}
