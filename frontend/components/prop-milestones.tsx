"use client";

import { OddsButton } from "@/components/odds-button";
import { milestoneSelection } from "@/lib/player-props";
import type { Match, PlayerProp } from "@/lib/types";

/** Paliers d'un pari joueur (« 30+ », « 35+ »…), chacun sa cote : rien s'il n'y en a pas. */
export function PropMilestones({ match, prop, className }: { match: Match; prop: PlayerProp; className?: string }) {
  if (!prop.milestones?.length) return null;
  return (
    <div className={className ?? "flex gap-1"}>
      {prop.milestones.map((m) => (
        <OddsButton key={m.threshold} selection={milestoneSelection(match, prop, m)} label={`${m.threshold}+`} />
      ))}
    </div>
  );
}
