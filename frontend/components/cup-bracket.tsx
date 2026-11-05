"use client";

/* eslint-disable @next/next/no-img-element */
import { Fragment } from "react";
import { getTeamColor } from "@/lib/team-colors";
import { useTeamsByAbbreviation } from "@/lib/use-teams";
import { cn } from "@/lib/utils";

/** Logo officiel de la ligue, servi par le même CDN que ceux des équipes. */
const NBA_LOGO = "https://a.espncdn.com/i/teamlogos/leagues/500/nba.png";

// Le tableau est dessiné sur une grille fixe de 600 × 470, mise à l'échelle de
// l'écran : l'Ouest à gauche, l'Est à droite, la finale au centre.
const W = 600;
const H = 470;
const TILE = 84;

export type SlotState = "normal" | "chosen" | "out" | "right" | "wrong" | "ghost" | "champion";

export interface BracketSlot {
  team: string | null;
  state?: SlotState;
  onPick?: () => void;
}

export interface BracketSlots {
  /** Quarts : deux paires à l'Ouest, deux à l'Est. */
  westQuarters: BracketSlot[];
  eastQuarters: BracketSlot[];
  westSemis: BracketSlot[];
  eastSemis: BracketSlot[];
  westFinalist: BracketSlot;
  eastFinalist: BracketSlot;
}

const EMPTY: BracketSlot = { team: null };

export const EMPTY_SLOTS: BracketSlots = {
  westQuarters: [EMPTY, EMPTY, EMPTY, EMPTY],
  eastQuarters: [EMPTY, EMPTY, EMPTY, EMPTY],
  westSemis: [EMPTY, EMPTY],
  eastSemis: [EMPTY, EMPTY],
  westFinalist: EMPTY,
  eastFinalist: EMPTY,
};

const QUARTER_Y = [60, 150, 300, 390];
const SEMI_Y = [105, 345];
const FINAL_Y = 225;
const QUARTER_X = 42;
const SEMI_X = 146;
const FINAL_X = 254;

function Tile({ slot, x, y }: { slot: BracketSlot; x: number; y: number }) {
  const teams = useTeamsByAbbreviation();
  const info = slot.team ? teams.get(slot.team) : undefined;
  const state = slot.state ?? "normal";
  const color = slot.team ? getTeamColor(slot.team) : undefined;
  const clickable = !!slot.team && !!slot.onPick;

  return (
    <button
      type="button"
      disabled={!clickable}
      onClick={slot.onPick}
      aria-pressed={state === "chosen" || state === "champion"}
      aria-label={slot.team ? (info?.name ?? slot.team) : "À déterminer"}
      className={cn(
        "absolute flex items-center justify-center overflow-hidden rounded-md transition-all",
        slot.team ? "shadow-[inset_0_0_0_1px_rgba(255,255,255,0.22)]" : "border border-white/25 bg-black/35",
        clickable && "hover:brightness-125 active:scale-95",
        state === "chosen" && "ring-2 ring-[#FF7A1A]",
        state === "champion" && "ring-[3px] ring-[#E8B64C] shadow-[0_0_18px_rgba(232,182,76,0.55)]",
        state === "right" && "ring-2 ring-emerald-400",
        state === "wrong" && "ring-2 ring-red-500",
        state === "out" && "opacity-35 grayscale",
        state === "ghost" && "opacity-45"
      )}
      style={{
        left: `${((x - TILE / 2) / W) * 100}%`,
        top: `${((y - TILE / 2) / H) * 100}%`,
        width: `${(TILE / W) * 100}%`,
        height: `${(TILE / H) * 100}%`,
        background: color ? `linear-gradient(160deg, ${color}, ${color}cc 55%, #0B1120)` : undefined,
      }}
    >
      {slot.team &&
        (info?.logoUrl ? (
          <img src={info.logoUrl} alt="" draggable={false} className="h-[78%] w-[78%] object-contain drop-shadow-[0_1px_2px_rgba(0,0,0,0.55)]" />
        ) : (
          <span className="text-[10px] font-bold text-white sm:text-sm">{slot.team}</span>
        ))}
    </button>
  );
}

// Les traits du tableau d'un côté ; `mirror` les retourne pour l'autre.
function Lines({ mirror }: { mirror: boolean }) {
  const px = (x: number) => (mirror ? W - x : x);
  const half = TILE / 2;
  return (
    <g stroke="rgba(255,255,255,0.4)" strokeWidth={1.5} fill="none" vectorEffect="non-scaling-stroke">
      {[0, 2].map((i) => (
        <Fragment key={i}>
          <line x1={px(QUARTER_X)} x2={px(QUARTER_X)} y1={QUARTER_Y[i] + half} y2={QUARTER_Y[i + 1] - half} />
          <line x1={px(QUARTER_X)} x2={px(SEMI_X - half)} y1={SEMI_Y[i / 2]} y2={SEMI_Y[i / 2]} />
        </Fragment>
      ))}
      <line x1={px(SEMI_X)} x2={px(SEMI_X)} y1={SEMI_Y[0] + half} y2={SEMI_Y[1] - half} />
      <line x1={px(SEMI_X)} x2={px(FINAL_X - half)} y1={FINAL_Y} y2={FINAL_Y} />
    </g>
  );
}

/** Le tableau façon NBA Cup. Mêmes cases à remplir, à lire ou vides : seules les données changent. */
export function CupBracket({ slots }: { slots: BracketSlots }) {
  return (
    <div className="island relative overflow-hidden rounded-2xl bg-[#080B12] p-2 sm:p-4">
      <div aria-hidden className="absolute inset-0 opacity-40" style={{ background: "repeating-linear-gradient(115deg, transparent 0 18px, rgba(255,255,255,0.025) 18px 19px), radial-gradient(70% 60% at 50% 45%, rgba(232,182,76,0.14), transparent 70%)" }} />
      <div className="relative w-full" style={{ aspectRatio: `${W} / ${H}` }}>
        <svg viewBox={`0 0 ${W} ${H}`} className="absolute inset-0 h-full w-full" aria-hidden>
          <Lines mirror={false} />
          <Lines mirror />
          <line x1={W / 2} x2={W / 2} y1={FINAL_Y} y2={290} stroke="rgba(232,182,76,0.7)" strokeWidth={1.5} vectorEffect="non-scaling-stroke" />
          <line x1={FINAL_X + TILE / 2} x2={W - FINAL_X - TILE / 2} y1={FINAL_Y} y2={FINAL_Y} stroke="rgba(232,182,76,0.7)" strokeWidth={1.5} vectorEffect="non-scaling-stroke" />
        </svg>

        <p className="absolute left-0 top-0 font-heading text-[9px] font-bold uppercase tracking-[.18em] text-[#E8B64C] sm:text-sm">Ouest</p>
        <p className="absolute right-0 top-0 font-heading text-[9px] font-bold uppercase tracking-[.18em] text-[#E8B64C] sm:text-sm">Est</p>

        <div className="absolute left-1/2 top-0 flex w-[34%] -translate-x-1/2 flex-col items-center gap-[2%] pt-[1%]">
          <img src={NBA_LOGO} alt="NBA" className="h-auto w-[30%] object-contain" />
          <p className="whitespace-nowrap font-heading text-[11px] font-extrabold uppercase italic leading-none tracking-wide text-white sm:text-2xl">Coupe NBA</p>
          <p className="whitespace-nowrap font-heading text-[8px] font-semibold uppercase tracking-[.2em] text-[#E8B64C] sm:text-xs">Finale</p>
        </div>

        <img src="/coupe/trophee.png" alt="Le trophée de la Coupe NBA" className="absolute left-1/2 top-[61%] h-[38%] w-auto -translate-x-1/2 drop-shadow-[0_6px_18px_rgba(232,182,76,0.35)]" />

        {slots.westQuarters.map((slot, i) => <Tile key={`wq${i}`} slot={slot} x={QUARTER_X} y={QUARTER_Y[i]} />)}
        {slots.eastQuarters.map((slot, i) => <Tile key={`eq${i}`} slot={slot} x={W - QUARTER_X} y={QUARTER_Y[i]} />)}
        {slots.westSemis.map((slot, i) => <Tile key={`ws${i}`} slot={slot} x={SEMI_X} y={SEMI_Y[i]} />)}
        {slots.eastSemis.map((slot, i) => <Tile key={`es${i}`} slot={slot} x={W - SEMI_X} y={SEMI_Y[i]} />)}
        <Tile slot={slots.westFinalist} x={FINAL_X} y={FINAL_Y} />
        <Tile slot={slots.eastFinalist} x={W - FINAL_X} y={FINAL_Y} />
      </div>
    </div>
  );
}
