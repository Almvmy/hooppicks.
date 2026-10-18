import { SITE_URL } from "@/lib/share";

const BACKEND_URL = process.env.BACKEND_API_URL ?? "http://localhost:3001";
// Durée réservée dans l'agenda : un match NBA dure environ 2 h 30.
const GAME_MS = 150 * 60 * 1000;

interface ScheduledGame {
  eventId: string;
  date: string;
  homeAbbreviation: string;
  awayAbbreviation: string;
  completed: boolean;
  homeScore: number;
  awayScore: number;
}

interface Team {
  abbreviation: string;
  name: string;
}

// 20261020T190000Z : heures en UTC (= GMT), l'agenda les affiche à l'heure du téléphone.
function icsDate(ms: number): string {
  return new Date(ms).toISOString().replace(/[-:]/g, "").replace(/\.\d{3}/, "");
}

function escape(text: string): string {
  return text.replace(/\\/g, "\\\\").replace(/;/g, "\\;").replace(/,/g, "\\,").replace(/\n/g, "\\n");
}

/**
 * Abonnement agenda aux matchs de saison régulière d'une équipe
 * (/calendrier/BOS.ics, ouvert en webcal:// depuis l'app) : l'agenda le relit
 * tout seul, les horaires et les scores suivent.
 */
export async function GET(_request: Request, { params }: { params: Promise<{ team: string }> }) {
  const abbr = (await params).team.replace(/\.ics$/i, "").toUpperCase();
  if (!/^[A-Z]{2,4}$/.test(abbr)) return new Response("Équipe inconnue", { status: 404 });

  const [scheduleRes, teamsRes] = await Promise.all([
    fetch(`${BACKEND_URL}/teams/${abbr}/schedule`, { next: { revalidate: 3600 } }).catch(() => null),
    fetch(`${BACKEND_URL}/teams`, { next: { revalidate: 3600 } }).catch(() => null),
  ]);
  if (!scheduleRes || scheduleRes.status === 404) return new Response("Équipe inconnue", { status: 404 });
  if (!scheduleRes.ok) return new Response("Calendrier indisponible, réessaie plus tard.", { status: 503 });

  const games = (await scheduleRes.json()) as ScheduledGame[];
  const teams = teamsRes?.ok ? ((await teamsRes.json()) as Team[]) : [];
  const name = (a: string) => teams.find((t) => t.abbreviation === a)?.name ?? a;
  const teamName = name(abbr);
  const stamp = icsDate(Date.now());

  const events = games.map((g) => {
    const start = new Date(g.date).getTime();
    const summary = g.completed
      ? `${name(g.awayAbbreviation)} ${g.awayScore} - ${g.homeScore} ${name(g.homeAbbreviation)}`
      : `${name(g.awayAbbreviation)} @ ${name(g.homeAbbreviation)}`;
    return [
      "BEGIN:VEVENT",
      `UID:${g.eventId || `${g.awayAbbreviation}-${g.homeAbbreviation}-${start}`}@hooppicks`,
      `DTSTAMP:${stamp}`,
      `DTSTART:${icsDate(start)}`,
      `DTEND:${icsDate(start + GAME_MS)}`,
      `SUMMARY:${escape(`🏀 ${summary}`)}`,
      `DESCRIPTION:${escape(`Pronostique ce match sur HoopPicks : ${SITE_URL}/matches`)}`,
      `URL:${SITE_URL}/matches`,
      "END:VEVENT",
    ].join("\r\n");
  });

  const body = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//HoopPicks//Calendrier equipe//FR",
    "CALSCALE:GREGORIAN",
    "METHOD:PUBLISH",
    `X-WR-CALNAME:${escape(`${teamName} · HoopPicks`)}`,
    "X-WR-TIMEZONE:UTC",
    "REFRESH-INTERVAL;VALUE=DURATION:PT12H",
    "X-PUBLISHED-TTL:PT12H",
    ...events,
    "END:VCALENDAR",
    "",
  ].join("\r\n");

  return new Response(body, {
    headers: {
      "Content-Type": "text/calendar; charset=utf-8",
      "Content-Disposition": `inline; filename="hooppicks-${abbr.toLowerCase()}.ics"`,
      "Cache-Control": "public, max-age=3600",
    },
  });
}
