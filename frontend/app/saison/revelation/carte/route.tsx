import { ImageResponse } from "next/og";
import { SeasonStatsArt } from "@/components/share-card-art";
import { fetchSharedSeasonPicks, fetchSharedTeams } from "@/lib/share";
import { shareFonts } from "@/lib/share-fonts";

const SHORT_LABEL: Record<string, string> = {
  champion: "Champion NBA",
  east_finalist: "Finaliste de l'Est",
  west_finalist: "Finaliste de l'Ouest",
  best_record: "Meilleur bilan",
};

// La répartition des pronostics de saison, 1080×1080 : seulement une fois
// verrouillés, comme sur la page (avant, on copierait les choix des autres).
export async function GET() {
  const [overview, teams, fonts] = await Promise.all([fetchSharedSeasonPicks(), fetchSharedTeams(), shareFonts()]);
  if (!overview) return new Response("Pronostics indisponibles", { status: 503 });
  if (!overview.locked) return new Response("Pas encore révélés", { status: 404 });
  const byAbbr = new Map((teams ?? []).map((t) => [t.abbreviation, t]));
  const rows = overview.questions.map((q) => ({
    label: SHORT_LABEL[q.key] ?? q.label,
    top: Object.entries(overview.community[q.key] ?? {})
      .slice(0, 3)
      .map(([abbreviation, pct]) => ({ abbreviation, pct, team: byAbbr.get(abbreviation) })),
  }));

  return new ImageResponse(<SeasonStatsArt season={overview.season} participants={overview.participants} rows={rows} />, {
    width: 1080,
    height: 1080,
    fonts,
    headers: { "Cache-Control": "no-store" },
  });
}
