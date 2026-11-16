"use client";

import type { ReactNode } from "react";
import { DialogTitle } from "@/components/ui/dialog";
import { TeamLogo } from "@/components/team-logo";
import { getTeamColor } from "@/lib/team-colors";
import type { Team } from "@/lib/types";
import { cn } from "@/lib/utils";

// Blocs communs aux fiches d'un joueur (sur un match, sur la saison) : même en-tête aux
// couleurs de l'équipe, mêmes grandes stats, même adresse en barres.

const POSITIONS: Record<string, string> = {
  PG: "Meneur",
  SG: "Arrière",
  G: "Arrière",
  SF: "Ailier",
  F: "Ailier",
  PF: "Ailier fort",
  C: "Pivot",
};

export function positionLabel(position: string | null | undefined): string | null {
  if (!position) return null;
  return POSITIONS[position] ?? position;
}

export function teamColor(team: Team | null | undefined): string {
  return team ? getTeamColor(team.abbreviation) : "#6B7280";
}

/** En-tête : photo détourée d'ESPN, numéro en filigrane, nom, équipe et étiquettes. */
export function PlayerHero({
  name,
  team,
  jersey,
  headshotUrl,
  details,
  chips,
}: {
  name: string;
  team: Team | null;
  jersey: string | null | undefined;
  headshotUrl: string | null | undefined;
  /** « #44 », « Pivot », « 2,11 m »… */
  details: (string | null | undefined | false)[];
  chips?: ReactNode;
}) {
  const color = teamColor(team);
  return (
    <div
      className="relative h-40 overflow-hidden rounded-t-2xl"
      style={{ background: `radial-gradient(120% 140% at 0% 100%, ${color}80 0%, ${color}26 45%, transparent 75%)` }}
    >
      {jersey && (
        <span aria-hidden className="absolute -bottom-6 right-2 font-heading text-[150px] font-bold leading-none opacity-[0.08]">
          {jersey}
        </span>
      )}
      {headshotUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={headshotUrl} alt="" className="absolute bottom-0 left-2 h-36 w-auto max-w-[40%] object-contain object-bottom" />
      ) : (
        <span className="absolute bottom-4 left-5 flex h-24 w-24 items-center justify-center rounded-full bg-tint/10 font-heading text-3xl font-bold text-muted-foreground">
          {jersey ?? "?"}
        </span>
      )}
      <div className="absolute inset-y-0 right-0 flex w-[56%] flex-col justify-center gap-1.5 pr-5">
        <DialogTitle className="pr-6 font-heading text-xl font-bold leading-tight">{name}</DialogTitle>
        <p className="flex flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
          {team && <TeamLogo abbreviation={team.abbreviation} logoUrl={team.logoUrl} size={16} />}
          {[team?.name, ...details].filter(Boolean).join(" · ")}
        </p>
        {chips && <div className="flex flex-wrap gap-1.5">{chips}</div>}
      </div>
    </div>
  );
}

/** Une statistique majeure, mise en avant (`highlight`) avec une petite mention dessous. */
export function BigStat({ label, value, note }: { label: string; value: string | number; note?: string | null }) {
  return (
    <div className="flex flex-col items-center gap-0.5">
      <span className={cn("font-heading text-4xl font-bold tabular-nums leading-none", note && "text-primary")}>{value}</span>
      <span className="text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">{label}</span>
      <span className={cn("text-[10px] font-semibold text-primary", !note && "invisible")}>{note ?? "-"}</span>
    </div>
  );
}

export function SmallStat({ label, value, className }: { label: string; value: string | number; className?: string }) {
  return (
    <div className="flex flex-col items-center gap-0.5 rounded-xl bg-tint/5 px-1 py-2">
      <span className={cn("font-mono text-base font-bold tabular-nums", className)}>{value}</span>
      <span className="text-[10px] uppercase tracking-wide text-muted-foreground">{label}</span>
    </div>
  );
}

/** Adresse en barre, remplie à hauteur du pourcentage. `detail` : « 10/13 » sur un match. */
export function ShootingBar({ label, pct, detail, color }: { label: string; pct: number | null; detail?: string; color: string }) {
  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-baseline justify-between text-sm">
        <span className="font-medium">{label}</span>
        <span className="font-mono tabular-nums">
          {detail && <span className="font-bold">{detail}</span>}
          <span className={cn(detail ? "ml-2 text-muted-foreground" : "font-bold")}>{pct === null ? "-" : `${Math.round(pct)} %`}</span>
        </span>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-tint/10" aria-hidden>
        <div className="h-full rounded-full" style={{ width: `${Math.max(0, Math.min(100, pct ?? 0))}%`, backgroundColor: color }} />
      </div>
    </div>
  );
}

export function SectionLabel({ children }: { children: ReactNode }) {
  return <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{children}</p>;
}
