// Dessins des cartes de partage, rendus par ImageResponse (Satori) :
//  - TicketArt : aperçu d'un ticket partagé (1200×630), un vrai coupon papier
//  - PlayerArt : aperçu d'un profil joueur (1200×630), une carte à collectionner
//  - RecapArt  : carte « récap de la semaine » en image carrée (1080×1080)
//  - PodiumArt : les 3 premiers du classement, en image carrée (1080×1080)
//  - ChampionArt : le champion de la semaine passée, en image carrée (1080×1080)
//  - SeasonPicksArt : les pronostics de saison d'un joueur (1080×1080)
//  - SeasonStatsArt : la répartition des pronostics de saison, après le verrou (1080×1080)
//  - SeasonTeaserArt : aperçu du lien vers la page Saison (1200×630)
// Satori : styles en ligne uniquement, chaque bloc en display flex, pas de
// classes ni de hooks. Polices : voir lib/share-fonts.ts (à passer à ImageResponse).
// Partout, le logo est le vrai : panier + ballon + tir en pointillés
// (variante « full », à garder ≥ ~110 px pour que les pointillés se voient).

import { LogoMark } from "@/app/logo-mark";
import { SITE_URL, SharedPlayer, SharedTicket, plainNumber, signedPlain } from "@/lib/share";
import type { LeaderboardEntry, WeekChampion } from "@/lib/types";
import { headlineFor } from "@/components/weekly-recap-card";
import { getTeamColor } from "@/lib/team-colors";

const NAVY = "#0B1120";
const NAVY2 = "#111A2E";
const ORANGE = "#FF7A1A";
const TEXT = "#F1F5F9";
const MUTED = "#8A98B0";
const GREEN = "#2FD071";
const RED = "#FF6B6B";
const PAPER = "#F4EFE4";
const INK = "#0B1120";
const PAPER_MUTED = "#6B6558";
const PAPER_LINE = "#D9D2C1";

// Titres, chiffres, étiquettes : condensée, capitales. Texte courant : Barlow.
const COND = "Barlow Condensed";
const BODY = "Barlow";

const HOST = (() => {
  try {
    return new URL(SITE_URL).host;
  } catch {
    return "hooppicks.vercel.app";
  }
})();

const COLORWAYS: Record<string, { from: string; to: string }> = {
  orange: { from: "#FF7A1A", to: "#C2410C" },
  purple: { from: "#A855F7", to: "#6D28D9" },
  blue: { from: "#3B82F6", to: "#1D4ED8" },
  green: { from: "#22C55E", to: "#15803D" },
  red: { from: "#EF4444", to: "#B91C1C" },
  teal: { from: "#14B8A6", to: "#0F766E" },
};

type Tone = "won" | "lost" | "void";
const TONE = {
  won: { onDark: GREEN, onPaper: "#15803D", slab: "#0E3B25", label: "Ticket gagnant", stamp: "Gagné" },
  lost: { onDark: RED, onPaper: "#C42B2B", slab: "#43191F", label: "Ticket perdu", stamp: "Perdu" },
  void: { onDark: "#B4C0D4", onPaper: "#5B6578", slab: "#1D2840", label: "Ticket remboursé", stamp: "Remboursé" },
} as const;

/* ---------------------------------------------------------------- briques */

/** Fond : marine uni, fines rayures en biais, grand pan de couleur. */
function Backdrop({ w, h, slab }: { w: number; h: number; slab?: { color: string; left: number; width: number } }) {
  return (
    <div
      style={{
        position: "absolute",
        left: 0,
        top: 0,
        width: w,
        height: h,
        display: "flex",
        overflow: "hidden",
        background: NAVY,
      }}
    >
      {slab && (
        <div
          style={{
            position: "absolute",
            left: slab.left,
            top: -40,
            width: slab.width,
            height: h + 80,
            display: "flex",
            background: slab.color,
            transform: "skewX(-9deg)",
          }}
        />
      )}
      <div
        style={{
          position: "absolute",
          left: 0,
          top: 0,
          width: w,
          height: h,
          display: "flex",
          backgroundImage: "repeating-linear-gradient(115deg, rgba(255,255,255,0.028) 0px, rgba(255,255,255,0.028) 2px, rgba(255,255,255,0) 2px, rgba(255,255,255,0) 26px)",
        }}
      />
    </div>
  );
}

/** Logo + nom. `mark` = taille du symbole (≥ 110 pour voir les pointillés). */
function Lockup({ mark, text, dark = true }: { mark: number; text: number; dark?: boolean }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: Math.round(mark * 0.12) }}>
      <LogoMark variant="full" rim={dark ? TEXT : INK} width={mark} height={mark} />
      <div
        style={{
          display: "flex",
          fontFamily: COND,
          fontWeight: 900,
          fontStyle: "italic",
          fontSize: text,
          lineHeight: 1,
          letterSpacing: 0.5,
          color: dark ? TEXT : INK,
        }}
      >
        <span>Hoop</span>
        <span style={{ color: ORANGE }}>Picks</span>
      </div>
    </div>
  );
}

/** Maillot : disque de l'avatar de l'app (numéro sur dégradé, double anneau). */
function Jersey({ number, size, colorway = "orange", ring = NAVY }: { number: number; size: number; colorway?: string; ring?: string }) {
  const p = COLORWAYS[colorway] ?? COLORWAYS.orange;
  const n = Number.isFinite(number) ? Math.max(0, Math.min(99, Math.round(number))) : 0;
  return (
    <div
      style={{
        width: size,
        height: size,
        borderRadius: size / 2,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: `linear-gradient(160deg, ${p.from} 0%, ${p.to} 100%)`,
        boxShadow: `0 0 0 ${Math.max(2, size * 0.05)}px ${ring}, 0 0 0 ${Math.max(4, size * 0.1)}px ${p.from}`,
        color: "white",
        fontFamily: COND,
        fontWeight: 900,
        fontStyle: "italic",
        fontSize: Math.round(size * 0.56),
        lineHeight: 1,
      }}
    >
      {n}
    </div>
  );
}

/** Étiquette en capitales condensées. */
function Label({ children, color = MUTED, size = 22, spacing = 4, weight = 700, upper = true }: { children: React.ReactNode; color?: string; size?: number; spacing?: number; weight?: 700 | 800; upper?: boolean }) {
  return (
    <div style={{ display: "flex", fontFamily: COND, fontWeight: weight, fontSize: size, lineHeight: 1, letterSpacing: spacing, textTransform: upper ? "uppercase" : "none", color }}>
      {children}
    </div>
  );
}

const frCote = (n: number) => n.toFixed(2).replace(".", ",");

function shortDate(iso: string | null): string {
  if (!iso) return "";
  try {
    return new Date(iso).toLocaleDateString("fr-FR", { day: "numeric", month: "long", timeZone: "Europe/Paris" });
  } catch {
    return "";
  }
}

/* ----------------------------------------------------------------- ticket */

export function TicketArt({ ticket }: { ticket: SharedTicket }) {
  const status = ticket.status as Tone;
  const t = TONE[status];
  const won = status === "won";
  const lost = status === "lost";
  const net = won ? ticket.potentialPayout - ticket.stake : lost ? -ticket.stake : 0;
  const legsAll = ticket.selections;
  const legs = legsAll.slice(0, legsAll.length > 4 ? 3 : 4);
  const hidden = legsAll.length - legs.length;
  const dense = legsAll.length >= 3;
  const solo = legs.length === 1;
  const combo = legsAll.length > 1;
  const numStr = status === "void" ? "0" : signedPlain(net);
  const numSize = numStr.length <= 2 ? 270 : numStr.length === 3 ? 250 : numStr.length === 4 ? 200 : numStr.length === 5 ? 176 : 148;
  const payoutLabel = won ? "Gain" : lost ? "Perdu" : "Remboursé";
  // Gagné : le bénéfice (gain − mise), ce qui compte au classement, comme le grand chiffre.
  const payoutText = won ? signedPlain(net) : plainNumber(ticket.stake);
  const payoutColor = lost ? RED : won ? GREEN : TEXT;

  return (
    <div style={{ position: "relative", display: "flex", width: 1200, height: 630, color: TEXT, fontFamily: BODY }}>
      <Backdrop w={1200} h={630} slab={{ color: t.slab, left: -140, width: 760 }} />

      {/* colonne gauche : résultat */}
      <div style={{ position: "relative", display: "flex", flexDirection: "column", justifyContent: "space-between", width: 540, padding: "44px 0 44px 56px" }}>
        <Lockup mark={138} text={58} />

        <div style={{ display: "flex", flexDirection: "column" }}>
          <div style={{ display: "flex" }}>
            <div style={{ display: "flex", padding: "7px 16px 6px", background: t.onDark, color: INK, fontFamily: COND, fontWeight: 800, fontSize: 27, lineHeight: 1, letterSpacing: 3.5, textTransform: "uppercase" }}>
              {t.label}
            </div>
          </div>
          <div style={{ display: "flex", alignItems: "flex-end", marginTop: 4 }}>
            <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: numSize, lineHeight: 0.92, letterSpacing: -2, color: t.onDark }}>{numStr}</div>
            <div style={{ display: "flex", fontFamily: COND, fontWeight: 800, fontSize: Math.round(numSize * 0.2), lineHeight: 1, letterSpacing: 3, color: TEXT, marginLeft: 14, marginBottom: Math.round(numSize * 0.1), textTransform: "uppercase" }}>pts</div>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 22, marginTop: 20 }}>
            <Jersey number={ticket.avatarNumber} size={50} colorway={ticket.avatarColorway} ring={t.slab} />
            <div style={{ display: "flex", fontFamily: COND, fontWeight: 800, fontSize: 44, lineHeight: 1, letterSpacing: 0.5 }}>{`@${ticket.username}`}</div>
          </div>
        </div>

        <Label color={MUTED} size={22} spacing={1.5} upper={false}>{HOST}</Label>
      </div>

      {/* colonne droite : le coupon papier */}
      <div style={{ position: "relative", display: "flex", flex: 1, padding: "40px 52px 40px 4px" }}>
        <div
          style={{
            position: "relative",
            display: "flex",
            flexDirection: "column",
            width: "100%",
            borderRadius: 8,
            background: PAPER,
            color: INK,
            transform: "rotate(1.4deg)",
            boxShadow: "0 28px 60px rgba(0,0,0,0.55)",
          }}
        >
          {/* en-tête */}
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "20px 28px 16px" }}>
            <div style={{ display: "flex", flexDirection: "column" }}>
              <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontSize: 40, lineHeight: 1, letterSpacing: 2, textTransform: "uppercase" }}>
                {combo ? `Combiné ×${legsAll.length}` : "Pari simple"}
              </div>
              <div style={{ display: "flex", fontFamily: BODY, fontWeight: 500, fontSize: 21, color: PAPER_MUTED, marginTop: 5 }}>
                {ticket.resolvedAt ? `Réglé le ${shortDate(ticket.resolvedAt)}` : "Points virtuels · sans argent"}
              </div>
            </div>
            <div
              style={{
                display: "flex",
                transform: "rotate(-7deg)",
                padding: "4px 16px 2px",
                border: `4px solid ${t.onPaper}`,
                color: t.onPaper,
                fontFamily: COND,
                fontWeight: 900,
                fontStyle: "italic",
                fontSize: t.stamp.length > 6 ? 40 : 52,
                lineHeight: 1,
                letterSpacing: 3,
                textTransform: "uppercase",
              }}
            >
              {t.stamp}
            </div>
          </div>

          {/* perforation + encoches */}
          <div style={{ position: "relative", display: "flex", height: 2, borderTop: `2px dashed ${PAPER_LINE}`, marginLeft: 22, marginRight: 22 }}>
            <div style={{ position: "absolute", left: -35, top: -17, width: 30, height: 30, borderRadius: 15, background: NAVY, display: "flex" }} />
            <div style={{ position: "absolute", right: -35, top: -17, width: 30, height: 30, borderRadius: 15, background: NAVY, display: "flex" }} />
          </div>

          {/* sélections */}
          <div style={{ display: "flex", flexDirection: "column", flex: 1, justifyContent: "center", padding: "0 28px" }}>
            {legs.map((s, i) => (
              <div
                key={i}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 18,
                  padding: dense ? (legs.length >= 4 ? "8px 0" : "11px 0") : solo ? "20px 0" : "16px 0",
                  borderBottom: i < legs.length - 1 || hidden > 0 ? `1.5px solid ${PAPER_LINE}` : "none",
                }}
              >
                <div style={{ display: "flex", width: solo ? 46 : 38, fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: solo ? 46 : dense ? 30 : 34, lineHeight: 1, color: ORANGE }}>{String(i + 1).padStart(2, "0")}</div>
                <div style={{ display: "flex", flexDirection: "column", flex: 1, minWidth: 0 }}>
                  <div
                    style={{
                      display: "block",
                      fontFamily: BODY,
                      fontWeight: 700,
                      fontSize: dense ? 27 : solo ? (s.label.length <= 16 ? 44 : s.label.length <= 30 ? 36 : 30) : 31,
                      lineHeight: 1.12,
                      ...(solo ? { lineClamp: 2 } : { whiteSpace: "nowrap" as const, overflow: "hidden", textOverflow: "ellipsis" }),
                    }}
                  >
                    {s.label}
                  </div>
                  {s.matchLabel && (
                    <div style={{ display: "block", fontFamily: BODY, fontWeight: 500, fontSize: solo ? 26 : dense ? 20 : 22, color: PAPER_MUTED, marginTop: solo ? 6 : 1, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                      {s.matchLabel}
                    </div>
                  )}
                </div>
                <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: dense ? 38 : solo ? 62 : 46, lineHeight: 1 }}>{frCote(s.odds)}</div>
              </div>
            ))}
            {hidden > 0 && (
              <div style={{ display: "flex", padding: "10px 0 2px", fontFamily: BODY, fontWeight: 500, fontSize: 23, color: PAPER_MUTED }}>
                {`+ ${hidden} autre${hidden > 1 ? "s" : ""} sélection${hidden > 1 ? "s" : ""}`}
              </div>
            )}
          </div>

          {/* totaux */}
          <div style={{ display: "flex", background: INK, borderRadius: "0 0 8px 8px" }}>
            {[
              { k: "Mise", v: plainNumber(ticket.stake), c: TEXT },
              { k: "Cote", v: `×${frCote(ticket.totalOdds)}`, c: TEXT },
              { k: payoutLabel, v: payoutText, c: payoutColor },
            ].map((x, i) => (
              <div key={x.k} style={{ display: "flex", flexDirection: "column", flex: 1, padding: "14px 0 14px 28px", borderLeft: i > 0 ? "1.5px solid rgba(255,255,255,0.12)" : "none" }}>
                <Label color={MUTED} size={18} spacing={3.5}>{x.k}</Label>
                <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 50, lineHeight: 1.05, marginTop: 4, color: x.c }}>{x.v}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

/* ----------------------------------------------------------------- joueur */

function Stat({ k, v, hint, accent = false, grow = 1 }: { k: string; v: string; hint?: string; accent?: boolean; grow?: number }) {
  const vs = v.length <= 3 ? 100 : v.length <= 4 ? 92 : v.length <= 6 ? 76 : 62;
  return (
    <div style={{ display: "flex", flexDirection: "column", flexGrow: grow, flexBasis: 0, minWidth: 0, paddingLeft: 18, borderLeft: `4px solid ${accent ? ORANGE : "rgba(255,255,255,0.16)"}` }}>
      <Label color={accent ? ORANGE : MUTED} size={22} spacing={4}>{k}</Label>
      <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: vs, lineHeight: 1, marginTop: 8, color: accent ? ORANGE : TEXT, whiteSpace: "nowrap" }}>{v}</div>
      <div style={{ display: "flex", fontFamily: BODY, fontWeight: 500, fontSize: 22, color: MUTED, marginTop: 6, minHeight: 26 }}>{hint ?? ""}</div>
    </div>
  );
}

// Surnom seul (« Thunder », « Trail Blazers ») : le nom complet ne tenait pas
// sur la carte. Thunder, Heat, Magic et Jazz se disent au singulier.
const SINGULAR_NICKNAMES = new Set(["Thunder", "Heat", "Magic", "Jazz"]);

function supporterOf(fullName: string): string {
  const nickname = fullName.endsWith("Trail Blazers") ? "Trail Blazers" : fullName.trim().split(/\s+/).pop() ?? fullName;
  return `Supporter ${SINGULAR_NICKNAMES.has(nickname) ? "du" : "des"} ${nickname}`;
}

export function PlayerArt({ player }: { player: SharedPlayer }) {
  const hasRank = player.seasonRank != null;
  const n = Math.max(0, Math.min(99, Math.round(player.avatarNumber)));
  const team = player.favoriteTeam ? supporterOf(player.favoriteTeam) : "Joueur HoopPicks";
  const nameSize = player.username.length <= 9 ? 46 : player.username.length <= 13 ? 38 : 30;
  return (
    <div style={{ position: "relative", display: "flex", width: 1200, height: 630, color: TEXT, fontFamily: BODY }}>
      <Backdrop w={1200} h={630} slab={{ color: "#3A1D0C", left: -140, width: 600 }} />

      {/* la carte à collectionner (et sa voisine derrière) */}
      <div style={{ position: "relative", display: "flex", width: 450, alignItems: "center", justifyContent: "center", paddingLeft: 24 }}>
        <div style={{ position: "absolute", left: 130, top: 62, width: 300, height: 480, display: "flex", borderRadius: 24, background: NAVY2, border: "2px solid rgba(255,255,255,0.14)", transform: "rotate(7deg)" }} />
        <div
          style={{
            position: "relative",
            display: "flex",
            flexDirection: "column",
            width: 322,
            height: 500,
            transform: "rotate(-4deg)",
            borderRadius: 26,
            overflow: "hidden",
            background: "linear-gradient(165deg, #FF8A2E 0%, #E2620F 55%, #A8480B 100%)",
            boxShadow: "0 34px 70px rgba(0,0,0,0.6)",
          }}
        >
          {/* filigrane */}
          <div style={{ position: "absolute", right: -26, top: 128, display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 360, lineHeight: 1, letterSpacing: -10, color: "rgba(255,255,255,0.13)" }}>{n}</div>
          {/* écusson : le vrai logo, couleurs de la marque */}
          <div style={{ position: "relative", display: "flex", alignItems: "center", justifyContent: "space-between", background: NAVY, padding: "12px 18px 10px 20px" }}>
            <div style={{ display: "flex", flexDirection: "column" }}>
              <Label color={ORANGE} size={19} spacing={4.5} weight={800}>Joueur</Label>
              <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 33, lineHeight: 1, letterSpacing: 0.5, marginTop: 6 }}>
                <span>Hoop</span>
                <span style={{ color: ORANGE }}>Picks</span>
              </div>
            </div>
            <LogoMark variant="full" rim={TEXT} width={118} height={118} />
          </div>
          <div style={{ position: "relative", display: "flex", flex: 1, flexDirection: "column", justifyContent: "flex-end", padding: "0 22px 22px" }}>
            <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 132, lineHeight: 0.9, letterSpacing: -2, color: "white" }}>{`#${n}`}</div>
            <div style={{ display: "block", marginTop: 10, fontFamily: COND, fontWeight: 800, fontSize: nameSize, lineHeight: 1.05, letterSpacing: 1.5, textTransform: "uppercase", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: 278 }}>{player.username}</div>
            <div style={{ display: "block", marginTop: 4, fontFamily: BODY, fontWeight: 500, fontSize: 21, color: "rgba(255,255,255,0.86)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: 278 }}>{team}</div>
          </div>
        </div>
      </div>

      {/* stats */}
      <div style={{ position: "relative", display: "flex", flexDirection: "column", justifyContent: "space-between", flex: 1, padding: "52px 60px 46px 28px" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <Label color={MUTED} size={24} spacing={5}>Saison en cours</Label>
          <Label color={MUTED} size={22} spacing={1.5} upper={false}>{HOST}</Label>
        </div>

        <div style={{ display: "flex", flexDirection: "column" }}>
          <div style={{ display: "block", fontFamily: COND, fontWeight: 800, fontSize: 84, lineHeight: 1, letterSpacing: 0.5, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: 670 }}>{`@${player.username}`}</div>
          <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 62, lineHeight: 1, letterSpacing: 1, color: ORANGE, marginTop: 10, textTransform: "uppercase" }}>Viens me défier.</div>
          <div style={{ display: "flex", gap: 22, marginTop: 40 }}>
            <Stat k="Classement" v={hasRank ? `#${player.seasonRank}` : "–"} hint={hasRank ? `sur ${player.seasonPlayers}` : "pas encore"} />
            <Stat k="Points" v={signedPlain(player.seasonPoints)} hint="cette saison" accent grow={1.45} />
            <Stat k="Réussite" v={player.settledTickets ? `${player.winRate}%` : "–"} hint={`${player.settledTickets} ticket${player.settledTickets > 1 ? "s" : ""}`} />
          </div>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
          <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 44, lineHeight: 1 }}>{player.bestStreak}</div>
          <Label color={MUTED} size={22} spacing={3.5}>{"pronostics gagnés d'affilée : mon record"}</Label>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ récap */

/**
 * La « carte de partage » de la page Profil, en image carrée (1080×1080) :
 * mêmes phrases et mêmes chiffres, pour l'envoyer en photo sur WhatsApp ou
 * en statut, là où un lien ne s'afficherait qu'en petit.
 */
export function RecapArt({ player, seasonLabel }: { player: SharedPlayer; seasonLabel: string }) {
  const [line1, line2] = headlineFor(player.currentStreak, player.weekPoints).split("\n");
  const wp = player.weekPoints;
  const l2 = line2.replace(/\.$/, "");
  // Une seule ligne quand c'est possible (≈ 0,42 em par capitale condensée), sinon plus petit.
  const l2Size = Math.min(220, Math.floor(880 / (Math.max(l2.length, 1) * 0.42)));
  const stats = [
    { k: "Points semaine", v: signedPlain(wp), color: wp > 0 ? "#15803D" : wp < 0 ? "#C42B2B" : INK, hint: "" },
    { k: "Classement", v: player.seasonRank ? `#${player.seasonRank}` : "–", color: INK, hint: player.seasonRank ? `sur ${player.seasonPlayers} joueurs` : "" },
    { k: "Réussite", v: player.settledTickets ? `${player.winRate}%` : "–", color: INK, hint: `${player.settledTickets} ticket${player.settledTickets > 1 ? "s" : ""} joué${player.settledTickets > 1 ? "s" : ""}` },
  ];
  return (
    <div style={{ position: "relative", display: "flex", width: 1080, height: 1080, color: TEXT, fontFamily: BODY }}>
      <Backdrop w={1080} h={1080} slab={{ color: wp < 0 ? "#1D2840" : "#3A1D0C", left: 560, width: 700 }} />
      {/* grand logo en filigrane */}
      <div style={{ position: "absolute", right: -70, top: 150, display: "flex", opacity: 0.16 }}>
        <LogoMark variant="full" rim={TEXT} width={560} height={560} />
      </div>

      <div style={{ position: "relative", display: "flex", flexDirection: "column", justifyContent: "space-between", width: 1080, height: 1080, padding: "64px 76px 64px" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <Lockup mark={140} text={66} />
          <Label color={MUTED} size={26} spacing={5}>{seasonLabel}</Label>
        </div>

        <div style={{ display: "flex", flexDirection: "column" }}>
          <div style={{ display: "flex", fontFamily: COND, fontWeight: 800, fontSize: 56, lineHeight: 1.02, letterSpacing: 2, textTransform: "uppercase", maxWidth: 900 }}>{line1}</div>
          <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: l2Size, lineHeight: 0.94, letterSpacing: -1, textTransform: "uppercase", color: ORANGE, maxWidth: 920, marginTop: 8 }}>{l2}</div>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 40 }}>
          {/* bandeau papier, comme le coupon */}
          <div style={{ display: "flex", background: PAPER, color: INK, transform: "rotate(-1deg)", boxShadow: "0 26px 56px rgba(0,0,0,0.5)", borderRadius: 8 }}>
            {stats.map((s, i) => (
              <div key={s.k} style={{ display: "flex", flexDirection: "column", flex: 1, padding: "22px 0 18px 28px", borderLeft: i > 0 ? `2px dashed ${PAPER_LINE}` : "none" }}>
                <Label color={PAPER_MUTED} size={21} spacing={3.5}>{s.k}</Label>
                <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: s.v.length <= 5 ? 112 : 92, lineHeight: 1, marginTop: 8, color: s.color, whiteSpace: "nowrap" }}>{s.v}</div>
                <div style={{ display: "flex", fontFamily: BODY, fontWeight: 500, fontSize: 22, color: PAPER_MUTED, minHeight: 28, marginTop: 4 }}>{s.hint}</div>
              </div>
            ))}
          </div>

          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 26 }}>
              <Jersey number={player.avatarNumber} size={70} />
              <div style={{ display: "flex", fontFamily: COND, fontWeight: 800, fontSize: 60, lineHeight: 1, letterSpacing: 0.5 }}>{`@${player.username}`}</div>
            </div>
            <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end" }}>
              <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 40, lineHeight: 1, color: ORANGE, textTransform: "uppercase", letterSpacing: 1 }}>Viens me défier</div>
              <Label color={MUTED} size={22} spacing={1.5} upper={false}>{HOST}</Label>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ----------------------------------------------------------------- podium */

// Or, argent, bronze : les mêmes que le podium de la page Classement.
const MEDAL = {
  1: { color: "#F5B700", deep: "#8A6400", glow: "rgba(245,183,0,0.32)" },
  2: { color: "#C3CCD8", deep: "#5E6B7D", glow: "rgba(195,204,216,0.2)" },
  3: { color: "#D98A3D", deep: "#7A4416", glow: "rgba(217,138,61,0.22)" },
} as const;

function Crown({ size, color }: { size: number; color: string }) {
  return (
    <svg width={size} height={Math.round(size * 0.78)} viewBox="-8 -8 116 86">
      <path d="M6 64 L0 14 L28 36 L50 0 L72 36 L100 14 L94 64 Z" fill={color} />
      <rect x="6" y="58" width="88" height="14" rx="3" fill={color} />
      <circle cx="0" cy="14" r="7" fill={color} />
      <circle cx="50" cy="2" r="7" fill={color} />
      <circle cx="100" cy="14" r="7" fill={color} />
    </svg>
  );
}

/** Une marche du podium : le joueur au-dessus, la marche (et son rang) en dessous. */
function PodiumStep({ entry, place }: { entry: LeaderboardEntry | undefined; place: 1 | 2 | 3 }) {
  const m = MEDAL[place];
  const first = place === 1;
  const stepHeight = { 1: 175, 2: 125, 3: 90 }[place];
  const jersey = first ? 140 : 112;
  const width = first ? 340 : 290;

  if (!entry) {
    return (
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", width }}>
        <div style={{ display: "flex", width: jersey, height: jersey, borderRadius: jersey / 2, border: "4px dashed rgba(255,255,255,0.22)" }} />
        <div style={{ display: "flex", fontFamily: COND, fontWeight: 700, fontSize: 34, color: MUTED, marginTop: 22, marginBottom: 26, textTransform: "uppercase", letterSpacing: 2 }}>Place libre</div>
        <div style={{ display: "flex", justifyContent: "center", width, height: stepHeight, background: "rgba(255,255,255,0.05)", borderRadius: "18px 18px 0 0", paddingTop: place === 3 ? 8 : 14 }}>
          <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: { 1: 132, 2: 96, 3: 70 }[place], lineHeight: 1, color: "rgba(255,255,255,0.18)" }}>{String(place)}</div>
        </div>
      </div>
    );
  }

  const name = entry.username;
  const nameSize = name.length <= 8 ? (first ? 54 : 44) : name.length <= 12 ? (first ? 44 : 36) : first ? 36 : 30;

  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", width }}>
      {first && (
        <div style={{ display: "flex", marginBottom: 10 }}>
          <Crown size={76} color={m.color} />
        </div>
      )}
      <Jersey number={entry.avatarNumber} size={jersey} colorway={entry.avatarColorway} ring={m.color} />
      <div style={{ display: "flex", maxWidth: width - 10, marginTop: first ? 22 : 20, fontFamily: COND, fontWeight: 800, fontSize: nameSize, lineHeight: 1, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{`@${name}`}</div>
      <div style={{ display: "flex", alignItems: "baseline", gap: 8, marginTop: 10 }}>
        <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: first ? 58 : 46, lineHeight: 1, color: first ? m.color : TEXT }}>{signedPlain(entry.points)}</div>
        <Label color={MUTED} size={first ? 24 : 20} spacing={2}>pts</Label>
      </div>
      <div style={{ display: "flex", fontFamily: BODY, fontWeight: 500, fontSize: first ? 24 : 21, color: MUTED, marginTop: 6, marginBottom: 20 }}>{`${entry.winRate}% de réussite`}</div>
      <div
        style={{
          position: "relative",
          display: "flex",
          justifyContent: "center",
          width,
          height: stepHeight,
          borderRadius: "18px 18px 0 0",
          background: `linear-gradient(180deg, ${m.color} 0%, ${m.deep} 100%)`,
          boxShadow: `0 -10px 60px ${m.glow}`,
          paddingTop: place === 3 ? 8 : 14,
        }}
      >
        {/* liseré clair en haut de la marche */}
        <div style={{ position: "absolute", left: 0, top: 0, width, height: 8, display: "flex", borderRadius: "18px 18px 0 0", background: "rgba(255,255,255,0.45)" }} />
        <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: { 1: 132, 2: 96, 3: 70 }[place], lineHeight: 1, color: "rgba(11,17,32,0.82)" }}>{String(entry.rank)}</div>
      </div>
    </div>
  );
}

/**
 * Le podium du classement en image carrée (1080×1080), pour l'envoyer en
 * photo ou en statut. `title` = la période (« de la semaine »…), `dateLabel`
 * = la date qui va avec, `total` = joueurs classés.
 */
export function PodiumArt({ entries, title, dateLabel, total }: { entries: LeaderboardEntry[]; title: string; dateLabel: string; total: number }) {
  const [first, second, third] = entries;
  const leader = first?.username;
  return (
    <div style={{ position: "relative", display: "flex", width: 1080, height: 1080, color: TEXT, fontFamily: BODY }}>
      <Backdrop w={1080} h={1080} />
      {/* faisceau doré sur la 1re place */}
      <div style={{ position: "absolute", left: 140, top: 160, width: 800, height: 800, display: "flex", background: "radial-gradient(circle at 50% 55%, rgba(245,183,0,0.24) 0%, rgba(245,183,0,0.08) 32%, rgba(245,183,0,0) 50%)" }} />
      {/* sol de la salle sous le podium */}
      <div style={{ position: "absolute", left: 0, top: 930, width: 1080, height: 150, display: "flex", background: "linear-gradient(180deg, #1A2440 0%, #0B1120 100%)" }} />

      <div style={{ position: "relative", display: "flex", flexDirection: "column", width: 1080, height: 1080, padding: "56px 70px 0" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <Lockup mark={118} text={56} />
          <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 8 }}>
            <Label color={ORANGE} size={24} spacing={5}>Classement</Label>
            <Label color={MUTED} size={24} spacing={3}>{dateLabel}</Label>
          </div>
        </div>

        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", marginTop: 10 }}>
          <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 96, lineHeight: 0.95, letterSpacing: -1, textTransform: "uppercase" }}>Le podium</div>
          <div style={{ display: "flex", fontFamily: COND, fontWeight: 800, fontSize: 40, lineHeight: 1, letterSpacing: 6, textTransform: "uppercase", color: ORANGE, marginTop: 8 }}>{title}</div>
        </div>
      </div>

      {/* 2e, 1er, 3e : le premier au centre, plus haut */}
      <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 930, display: "flex", alignItems: "flex-end", justifyContent: "center", gap: 18 }}>
        <PodiumStep entry={second} place={2} />
        <PodiumStep entry={first} place={1} />
        <PodiumStep entry={third} place={3} />
      </div>

      <div style={{ position: "absolute", left: 0, top: 930, width: 1080, height: 150, display: "flex", alignItems: "center", justifyContent: "space-between", padding: "0 70px" }}>
        <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
          <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 44, lineHeight: 1, textTransform: "uppercase", color: ORANGE, maxWidth: 640, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
            {leader ? `Qui va détrôner @${leader} ?` : "La place est à prendre"}
          </div>
          <Label color={MUTED} size={22} spacing={2}>{`${plainNumber(total)} joueur${total > 1 ? "s" : ""} classé${total > 1 ? "s" : ""}`}</Label>
        </div>
        <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 10 }}>
          <Label color={TEXT} size={26} spacing={1.5} upper={false}>{HOST}</Label>
          <Label color={MUTED} size={20} spacing={2}>Pronostics NBA · sans argent</Label>
        </div>
      </div>
    </div>
  );
}

/* --------------------------------------------------------------- champion */

/**
 * Le ou les champions de la semaine passée, en image carrée (1080×1080).
 * Un seul : grand maillot au centre. Ex æquo : jusqu'à 3 maillots côte à côte.
 */
export function ChampionArt({ champions, weekLabel }: { champions: WeekChampion[]; weekLabel: string }) {
  const gold = MEDAL[1];
  const solo = champions.length === 1;
  const shown = champions.slice(0, 3);
  const c = shown[0];
  const nameSize = !c ? 100 : c.username.length <= 8 ? 128 : c.username.length <= 12 ? 104 : 82;
  return (
    <div style={{ position: "relative", display: "flex", width: 1080, height: 1080, color: TEXT, fontFamily: BODY }}>
      <Backdrop w={1080} h={1080} slab={{ color: "#2A2108", left: 600, width: 680 }} />
      <div style={{ position: "absolute", left: 90, top: 150, width: 900, height: 900, display: "flex", background: "radial-gradient(circle at 50% 42%, rgba(245,183,0,0.26) 0%, rgba(245,183,0,0.07) 34%, rgba(245,183,0,0) 52%)" }} />

      <div style={{ position: "relative", display: "flex", flexDirection: "column", width: 1080, height: 1080, padding: "56px 70px 56px" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <Lockup mark={118} text={56} />
          <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 8 }}>
            <Label color={gold.color} size={24} spacing={5}>Palmarès</Label>
            <Label color={MUTED} size={24} spacing={3}>{weekLabel}</Label>
          </div>
        </div>

        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", flex: 1, justifyContent: "center", marginTop: 10 }}>
          <Crown size={solo ? 120 : 96} color={gold.color} />
          <div style={{ display: "flex", fontFamily: COND, fontWeight: 800, fontSize: 40, letterSpacing: 7, textTransform: "uppercase", color: gold.color, marginTop: 14 }}>
            {solo ? "Champion de la semaine" : "Champions de la semaine"}
          </div>

          {solo && c ? (
            <div style={{ display: "flex", flexDirection: "column", alignItems: "center", marginTop: 34 }}>
              <Jersey number={c.avatarNumber} size={230} colorway={c.avatarColorway} ring={gold.color} />
              <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: nameSize, lineHeight: 1, marginTop: 40, maxWidth: 940, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{`@${c.username}`}</div>
              <div style={{ display: "flex", alignItems: "baseline", gap: 12, marginTop: 14 }}>
                <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 96, lineHeight: 1, color: gold.color }}>{signedPlain(c.points)}</div>
                <Label color={MUTED} size={34} spacing={3}>pts</Label>
              </div>
              {c.favoriteTeam && (
                <div style={{ display: "flex", fontFamily: BODY, fontWeight: 500, fontSize: 28, color: MUTED, marginTop: 12 }}>{supporterOf(c.favoriteTeam)}</div>
              )}
            </div>
          ) : (
            <div style={{ display: "flex", gap: 56, marginTop: 48 }}>
              {shown.map((p) => (
                <div key={p.username} style={{ display: "flex", flexDirection: "column", alignItems: "center", width: 260 }}>
                  <Jersey number={p.avatarNumber} size={170} colorway={p.avatarColorway} ring={gold.color} />
                  <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: p.username.length <= 9 ? 54 : 42, lineHeight: 1, marginTop: 30, maxWidth: 260, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{`@${p.username}`}</div>
                  <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 56, lineHeight: 1, color: gold.color, marginTop: 10 }}>{signedPlain(p.points)}</div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 44, lineHeight: 1, textTransform: "uppercase", color: ORANGE }}>
            {solo ? "Qui va le détrôner ?" : "Qui va les détrôner ?"}
          </div>
          <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 10 }}>
            <Label color={TEXT} size={26} spacing={1.5} upper={false}>{HOST}</Label>
            <Label color={MUTED} size={20} spacing={2}>Pronostics NBA · sans argent</Label>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------ introuvable */

/** Aperçu quand le ticket ou le joueur n'existe plus (lien périmé ou mal copié). */
export function NotFoundArt({ title, hint }: { title: string; hint: string }) {
  return (
    <div style={{ position: "relative", display: "flex", width: 1200, height: 630, color: TEXT, fontFamily: BODY }}>
      <Backdrop w={1200} h={630} slab={{ color: "#1D2840", left: 380, width: 560 }} />
      <div style={{ position: "relative", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", width: 1200, height: 630 }}>
        <LogoMark variant="full" rim={TEXT} width={230} height={230} />
        <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 92, lineHeight: 1, letterSpacing: 1, textTransform: "uppercase", marginTop: 20 }}>{title}</div>
        <div style={{ display: "flex", fontFamily: BODY, fontWeight: 500, fontSize: 30, color: MUTED, marginTop: 14 }}>{hint}</div>
        <Label color={ORANGE} size={26} spacing={1.5} upper={false}>{HOST}</Label>
      </div>
    </div>
  );
}

/* ------------------------------------------------------ pronostics saison */

export interface SeasonArtTeam {
  abbreviation: string;
  name: string;
  logoUrl: string | null;
}

/** Logo d'équipe sur un disque à sa couleur ; sigle à la place si le logo manque. */
function TeamDisc({ team, size }: { team: SeasonArtTeam | undefined; size: number }) {
  const color = team ? getTeamColor(team.abbreviation) : "#3B4660";
  return (
    <div
      style={{
        width: size,
        height: size,
        borderRadius: size / 2,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: `radial-gradient(circle at 50% 40%, ${color} 0%, ${NAVY2} 78%)`,
        boxShadow: `0 0 0 ${Math.max(3, size * 0.025)}px rgba(255,255,255,0.14), 0 ${size * 0.08}px ${size * 0.3}px ${color}55`,
      }}
    >
      {team?.logoUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={team.logoUrl} width={Math.round(size * 0.72)} height={Math.round(size * 0.72)} alt="" style={{ objectFit: "contain" }} />
      ) : (
        <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: Math.round(size * 0.34), color: TEXT }}>
          {team?.abbreviation ?? "?"}
        </div>
      )}
    </div>
  );
}

function SeasonFooter({ left }: { left: string }) {
  return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
      <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 44, lineHeight: 1, textTransform: "uppercase", color: ORANGE }}>
        {left}
      </div>
      <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 10 }}>
        <Label color={TEXT} size={26} spacing={1.5} upper={false}>{`${HOST}/saison`}</Label>
        <Label color={MUTED} size={20} spacing={2}>Pronostics NBA · sans argent</Label>
      </div>
    </div>
  );
}

/**
 * Les pronostics de saison d'un joueur (1080×1080) : le champion en grand,
 * les finalistes et le meilleur bilan en dessous. Choix lus dans l'adresse
 * (le joueur partage les siens), jamais dans la base : rien n'est exposé avant le verrou.
 */
export function SeasonPicksArt({
  username,
  season,
  picks,
}: {
  username: string | null;
  season: string;
  picks: { champion?: SeasonArtTeam; east?: SeasonArtTeam; west?: SeasonArtTeam; best?: SeasonArtTeam };
}) {
  const champColor = picks.champion ? getTeamColor(picks.champion.abbreviation) : ORANGE;
  const small = [
    { label: "Finaliste Est", team: picks.east },
    { label: "Finaliste Ouest", team: picks.west },
    { label: "Meilleur bilan", team: picks.best },
  ];
  return (
    <div style={{ position: "relative", display: "flex", width: 1080, height: 1080, color: TEXT, fontFamily: BODY }}>
      <Backdrop w={1080} h={1080} slab={{ color: "#162036", left: 560, width: 700 }} />
      <div style={{ position: "absolute", left: 140, top: 120, width: 800, height: 640, display: "flex", background: `radial-gradient(circle at 50% 50%, ${champColor}40 0%, ${champColor}12 36%, rgba(0,0,0,0) 62%)` }} />

      <div style={{ position: "relative", display: "flex", flexDirection: "column", width: 1080, height: 1080, padding: "56px 70px 56px" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <Lockup mark={118} text={56} />
          <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 8 }}>
            <Label color={ORANGE} size={24} spacing={5}>Pronostics de saison</Label>
            <Label color={MUTED} size={24} spacing={3}>{`Saison ${season}`}</Label>
          </div>
        </div>

        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", marginTop: 24 }}>
          <Label color={MUTED} size={30} spacing={6}>{username ? `Les pronos de @${username}` : "Mes pronos"}</Label>
          <div style={{ display: "flex", marginTop: 28 }}>
            <TeamDisc team={picks.champion} size={300} />
          </div>
          <div style={{ display: "flex", marginTop: 30 }}>
            <Label color={ORANGE} size={34} spacing={8} weight={800}>Champion NBA</Label>
          </div>
          <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 104, lineHeight: 1, textTransform: "uppercase", marginTop: 10, maxWidth: 940, whiteSpace: "nowrap", overflow: "hidden" }}>
            {picks.champion?.name ?? "À choisir"}
          </div>
        </div>

        <div style={{ display: "flex", justifyContent: "space-between", marginTop: 40, flex: 1 }}>
          {small.map(({ label, team }) => (
            <div key={label} style={{ display: "flex", alignItems: "center", gap: 18, width: 300, padding: "18px 20px", borderRadius: 24, background: "rgba(255,255,255,0.05)", border: "2px solid rgba(255,255,255,0.08)", height: 150 }}>
              <TeamDisc team={team} size={96} />
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                <Label color={MUTED} size={20} spacing={2}>{label}</Label>
                <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 40, lineHeight: 1, textTransform: "uppercase", maxWidth: 160, whiteSpace: "nowrap", overflow: "hidden" }}>
                  {team?.name ?? "—"}
                </div>
              </div>
            </div>
          ))}
        </div>

        <SeasonFooter left="Et toi, qui sera champion ?" />
      </div>
    </div>
  );
}

/**
 * Ce que pense HoopPicks (1080×1080) : pour chaque question, les 3 équipes
 * les plus choisies avec leur part. Seulement après le verrou.
 */
export function SeasonStatsArt({
  season,
  participants,
  rows,
}: {
  season: string;
  participants: number;
  rows: { label: string; top: { team: SeasonArtTeam | undefined; abbreviation: string; pct: number }[] }[];
}) {
  return (
    <div style={{ position: "relative", display: "flex", width: 1080, height: 1080, color: TEXT, fontFamily: BODY }}>
      <Backdrop w={1080} h={1080} />
      <div style={{ position: "absolute", left: 600, top: -120, width: 700, height: 700, display: "flex", background: "radial-gradient(circle at 50% 50%, rgba(255,122,26,0.22) 0%, rgba(255,122,26,0) 60%)" }} />

      <div style={{ position: "relative", display: "flex", flexDirection: "column", width: 1080, height: 1080, padding: "56px 70px 56px" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <Lockup mark={118} text={56} />
          <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 8 }}>
            <Label color={ORANGE} size={24} spacing={5}>Pronostics de saison</Label>
            <Label color={MUTED} size={24} spacing={3}>{`Saison ${season}`}</Label>
          </div>
        </div>

        <div style={{ display: "flex", flexDirection: "column", marginTop: 26 }}>
          <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 84, lineHeight: 0.95, textTransform: "uppercase" }}>La révélation</div>
          <Label color={MUTED} size={26} spacing={3}>{`Les choix de ${plainNumber(participants)} joueur${participants > 1 ? "s" : ""}`}</Label>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 22, marginTop: 30, flex: 1 }}>
          {rows.map((row) => (
            <div key={row.label} style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              <Label color={ORANGE} size={22} spacing={4} weight={800}>{row.label}</Label>
              <div style={{ display: "flex", gap: 14 }}>
                {row.top.map((t, i) => (
                  <div key={t.abbreviation} style={{ position: "relative", display: "flex", alignItems: "center", gap: 14, width: 300, height: 82, padding: "0 16px", borderRadius: 18, overflow: "hidden", background: "rgba(255,255,255,0.05)" }}>
                    <div style={{ position: "absolute", left: 0, top: 0, width: Math.max(8, Math.round((t.pct / 100) * 300)), height: 82, display: "flex", background: i === 0 ? "rgba(255,122,26,0.30)" : "rgba(255,255,255,0.08)" }} />
                    <TeamDisc team={t.team} size={56} />
                    <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 34, lineHeight: 1, textTransform: "uppercase", width: 90, whiteSpace: "nowrap", overflow: "hidden" }}>
                      {t.abbreviation}
                    </div>
                    <div style={{ display: "flex", flexShrink: 0, whiteSpace: "nowrap", fontFamily: COND, fontWeight: 900, fontSize: 40, lineHeight: 1, color: i === 0 ? ORANGE : TEXT, marginLeft: "auto" }}>{`${t.pct}%`}</div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        <SeasonFooter left="Rendez-vous en juin" />
      </div>
    </div>
  );
}

/** Aperçu du lien vers la page Saison (1200×630). */
export function SeasonTeaserArt({ season, participants, revealed }: { season: string; participants: number; revealed: boolean }) {
  return (
    <div style={{ position: "relative", display: "flex", width: 1200, height: 630, color: TEXT, fontFamily: BODY }}>
      <Backdrop w={1200} h={630} slab={{ color: "#1D2840", left: 700, width: 600 }} />
      <div style={{ position: "absolute", left: 640, top: -80, width: 700, height: 700, display: "flex", background: "radial-gradient(circle at 50% 50%, rgba(255,122,26,0.28) 0%, rgba(255,122,26,0) 60%)" }} />
      <div style={{ position: "relative", display: "flex", flexDirection: "column", justifyContent: "space-between", width: 1200, height: 630, padding: "50px 64px" }}>
        <Lockup mark={112} text={52} />
        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          <Label color={ORANGE} size={26} spacing={6}>{`Pronostics de saison ${season}`}</Label>
          <div style={{ display: "flex", fontFamily: COND, fontWeight: 900, fontStyle: "italic", fontSize: 108, lineHeight: 0.92, textTransform: "uppercase" }}>
            {revealed ? "Qui voit qui champion ?" : "Qui sera champion ?"}
          </div>
          <div style={{ display: "flex", fontFamily: BODY, fontWeight: 500, fontSize: 30, color: MUTED }}>
            {revealed
              ? "Les choix de tous les joueurs sont révélés."
              : participants > 0
                ? `${plainNumber(participants)} joueur${participants > 1 ? "s ont" : " a"} déjà pronostiqué. À ton tour.`
                : "Champion, finalistes, meilleur bilan : à toi de jouer."}
          </div>
        </div>
        <Label color={TEXT} size={26} spacing={1.5} upper={false}>{`${HOST}/saison`}</Label>
      </div>
    </div>
  );
}
