// Dessins des images d'aperçu (1200×630), rendus par ImageResponse (Satori) :
// styles en ligne uniquement, chaque bloc en display flex, pas de classes.

import { SharedPlayer, SharedTicket, plainNumber, signedPlain } from "@/lib/share";
import { headlineFor } from "@/components/weekly-recap-card";

const NAVY = "#0B1120";
const ORANGE = "#FF7A1A";
const MUTED = "#94A3B8";
const GREEN = "#22C55E";
const RED = "#F87171";

function Frame({ children }: { children: React.ReactNode }) {
  return (
    <div
      style={{
        width: "100%",
        height: "100%",
        display: "flex",
        flexDirection: "column",
        justifyContent: "space-between",
        padding: "56px 64px",
        background: `radial-gradient(circle at 85% 15%, rgba(255,122,26,0.35), transparent 55%), ${NAVY}`,
        color: "white",
        fontFamily: "sans-serif",
      }}
    >
      {children}
      <div style={{ display: "flex", alignItems: "center", gap: 14, fontSize: 28, color: MUTED }}>
        <div
          style={{
            width: 44,
            height: 44,
            borderRadius: 22,
            background: ORANGE,
            color: NAVY,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontWeight: 800,
            fontSize: 20,
          }}
        >
          HP
        </div>
        <span>HoopPicks · pronostics NBA entre amis, en points virtuels</span>
      </div>
    </div>
  );
}

function Jersey({ number }: { number: number }) {
  return (
    <div
      style={{
        width: 96,
        height: 96,
        borderRadius: 48,
        border: `5px solid ${ORANGE}`,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        fontSize: 44,
        fontWeight: 800,
      }}
    >
      {number}
    </div>
  );
}

export function TicketArt({ ticket }: { ticket: SharedTicket }) {
  const net = ticket.status === "won" ? ticket.potentialPayout - ticket.stake : ticket.status === "lost" ? -ticket.stake : 0;
  const title = ticket.status === "won" ? "Ticket gagnant" : ticket.status === "lost" ? "Ticket perdu" : "Ticket remboursé";
  const color = ticket.status === "won" ? GREEN : ticket.status === "lost" ? RED : MUTED;
  const legs = ticket.selections.slice(0, 4);
  return (
    <Frame>
      <div style={{ display: "flex", alignItems: "center", gap: 24 }}>
        <Jersey number={ticket.avatarNumber} />
        <div style={{ display: "flex", flexDirection: "column" }}>
          <div style={{ fontSize: 44, fontWeight: 800 }}>{`@${ticket.username}`}</div>
          <div style={{ fontSize: 30, color }}>
            {`${title}${ticket.selections.length > 1 ? ` · combiné ×${ticket.selections.length}` : ""}`}
          </div>
        </div>
        <div style={{ marginLeft: "auto", fontSize: 96, fontWeight: 800, color }}>
          {ticket.status === "void" ? "=" : `${signedPlain(net)}`}
        </div>
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
        {legs.map((s, i) => (
          <div key={i} style={{ display: "flex", justifyContent: "space-between", fontSize: 34 }}>
            <span style={{ fontWeight: 700 }}>{s.label}</span>
            <span style={{ color: MUTED }}>{s.matchLabel ?? ""}</span>
          </div>
        ))}
        {ticket.selections.length > legs.length && (
          <div style={{ fontSize: 28, color: MUTED }}>{`+ ${ticket.selections.length - legs.length} autre(s) sélection(s)`}</div>
        )}
        <div style={{ fontSize: 30, color: MUTED }}>
          {`mise ${plainNumber(ticket.stake)} pts · cote ${ticket.totalOdds.toFixed(2).replace(".", ",")}`}
        </div>
      </div>
    </Frame>
  );
}

export function PlayerArt({ player }: { player: SharedPlayer }) {
  return (
    <Frame>
      <div style={{ display: "flex", alignItems: "center", gap: 28 }}>
        <Jersey number={player.avatarNumber} />
        <div style={{ display: "flex", flexDirection: "column" }}>
          <div style={{ fontSize: 60, fontWeight: 800 }}>{`@${player.username}`}</div>
          <div style={{ fontSize: 30, color: MUTED }}>{player.favoriteTeam ? `Supporter des ${player.favoriteTeam}` : "Joueur HoopPicks"}</div>
        </div>
      </div>
      <div style={{ display: "flex", gap: 64 }}>
        {[
          { label: "classement saison", value: player.seasonRank ? `#${player.seasonRank}` : "—", hint: player.seasonRank ? `sur ${player.seasonPlayers}` : "" },
          { label: "points", value: signedPlain(player.seasonPoints), hint: "" },
          { label: "réussite", value: player.settledTickets ? `${player.winRate}%` : "—", hint: `${player.settledTickets} tickets` },
          { label: "meilleure série", value: `${player.bestStreak}`, hint: "d'affilée" },
        ].map((s) => (
          <div key={s.label} style={{ display: "flex", flexDirection: "column" }}>
            <div style={{ fontSize: 26, color: MUTED, textTransform: "uppercase" }}>{s.label}</div>
            <div style={{ fontSize: 72, fontWeight: 800, color: s.label === "points" ? ORANGE : "white" }}>{s.value}</div>
            <div style={{ fontSize: 24, color: MUTED }}>{s.hint}</div>
          </div>
        ))}
      </div>
    </Frame>
  );
}

/**
 * La « carte de partage » de la page Profil, en image carrée (1080×1080) :
 * mêmes phrases et mêmes chiffres, pour l'envoyer en photo sur WhatsApp ou
 * en statut, là où un lien ne s'afficherait qu'en petit.
 */
export function RecapArt({ player, seasonLabel }: { player: SharedPlayer; seasonLabel: string }) {
  const [line1, line2] = headlineFor(player.currentStreak, player.weekPoints).split("\n");
  const stats = [
    { label: "POINTS SEMAINE", value: signedPlain(player.weekPoints), color: player.weekPoints > 0 ? GREEN : "white" },
    { label: player.seasonRank ? `CLASSEMENT / ${player.seasonPlayers}` : "CLASSEMENT", value: player.seasonRank ? `#${player.seasonRank}` : "—", color: "white" },
    { label: "RÉUSSITE", value: player.settledTickets ? `${player.winRate}%` : "—", color: "white" },
  ];
  return (
    <div
      style={{
        width: "100%",
        height: "100%",
        display: "flex",
        flexDirection: "column",
        justifyContent: "space-between",
        padding: 80,
        background: `radial-gradient(circle at 90% 90%, rgba(255,122,26,0.30), transparent 55%), linear-gradient(135deg, #16213A, ${NAVY})`,
        color: "white",
        fontFamily: "sans-serif",
      }}
    >
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 18, fontSize: 44, fontWeight: 800 }}>
          <div style={{ width: 64, height: 64, borderRadius: 32, background: ORANGE, color: NAVY, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 28 }}>
            HP
          </div>
          <div style={{ display: "flex" }}>
            <span>Hoop</span>
            <span style={{ color: ORANGE }}>Picks</span>
          </div>
        </div>
        <div style={{ fontSize: 28, color: MUTED, letterSpacing: 3 }}>{seasonLabel.toUpperCase()}</div>
      </div>
      <div style={{ display: "flex", flexDirection: "column", fontSize: 84, fontWeight: 800, lineHeight: 1.1 }}>
        <span>{line1}</span>
        <span style={{ color: ORANGE }}>{line2}</span>
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 40 }}>
        <div style={{ display: "flex", gap: 64 }}>
          {stats.map((s) => (
            <div key={s.label} style={{ display: "flex", flexDirection: "column" }}>
              <div style={{ fontSize: 72, fontWeight: 800, color: s.color }}>{s.value}</div>
              <div style={{ fontSize: 24, color: MUTED, letterSpacing: 2 }}>{s.label}</div>
            </div>
          ))}
        </div>
        <div style={{ display: "flex", justifyContent: "space-between", fontSize: 36, color: MUTED }}>
          <span>{`@${player.username}`}</span>
          <span>hooppicks · viens me défier</span>
        </div>
      </div>
    </div>
  );
}
