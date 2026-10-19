import { ImageResponse } from "next/og";
import { PodiumArt } from "@/components/share-card-art";
import { fetchSharedLeaderboard } from "@/lib/share";
import { shareFonts } from "@/lib/share-fonts";
import { seasonLabel } from "@/lib/utils";

type Period = "season" | "month" | "week" | "cup" | "playoffs";

const DAY_MS = 24 * 60 * 60 * 1000;

/** Début de la semaine de jeu en cours : lundi 12h GMT (cf. BankrollService). */
function weekStart(now: Date): Date {
  const monday = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate(), 12));
  monday.setTime(monday.getTime() - ((monday.getUTCDay() + 6) % 7) * DAY_MS);
  if (monday.getTime() > now.getTime()) monday.setTime(monday.getTime() - 7 * DAY_MS);
  return monday;
}

function labels(period: Period, now: Date): { title: string; dateLabel: string } {
  if (period === "week") {
    const day = weekStart(now).toLocaleDateString("fr-FR", { day: "numeric", month: "long", timeZone: "UTC" });
    return { title: "de la semaine", dateLabel: `Semaine du ${day}` };
  }
  if (period === "month") {
    return { title: "du mois", dateLabel: now.toLocaleDateString("fr-FR", { month: "long", year: "numeric", timeZone: "UTC" }) };
  }
  if (period === "cup") return { title: "de la Coupe NBA", dateLabel: `Saison ${seasonLabel(now)}` };
  if (period === "playoffs") return { title: "des playoffs", dateLabel: `Saison ${seasonLabel(now)}` };
  return { title: "de la saison", dateLabel: `Saison ${seasonLabel(now)}` };
}

// Image du podium (page Classement), 1080×1080 : envoyée en photo par
// « Partager l'image ». Route publique, comme le classement lui-même.
export async function GET(request: Request) {
  const raw = new URL(request.url).searchParams.get("periode");
  const period: Period = raw === "week" || raw === "month" || raw === "cup" || raw === "playoffs" ? raw : "season";
  const [entries, fonts] = await Promise.all([fetchSharedLeaderboard(period), shareFonts()]);
  if (!entries) return new Response("Classement indisponible", { status: 503 });

  const { title, dateLabel } = labels(period, new Date());
  return new ImageResponse(<PodiumArt entries={entries.slice(0, 3)} title={title} dateLabel={dateLabel} total={entries.length} />, {
    width: 1080,
    height: 1080,
    fonts,
    headers: { "Cache-Control": "no-store" },
  });
}
