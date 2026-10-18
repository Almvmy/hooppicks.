import { ImageResponse } from "next/og";
import { SeasonPicksArt } from "@/components/share-card-art";
import { fetchSharedTeams } from "@/lib/share";
import { shareFonts } from "@/lib/share-fonts";

// Les pronostics de saison d'un joueur, 1080×1080, envoyés en photo par
// « Partager mes pronos ». Les choix viennent de l'adresse (le joueur partage
// les siens) : la route ne lit jamais les pronostics des autres dans la base.
export async function GET(request: Request) {
  const params = new URL(request.url).searchParams;
  const [teams, fonts] = await Promise.all([fetchSharedTeams(), shareFonts()]);
  const byAbbr = new Map((teams ?? []).map((t) => [t.abbreviation, t]));
  const team = (key: string) => {
    const abbr = params.get(key);
    return abbr ? byAbbr.get(abbr) : undefined;
  };
  const raw = params.get("u");
  const username = raw && /^[\p{L}\p{N}_.-]{1,30}$/u.test(raw) ? raw : null;
  const rawSeason = params.get("s");
  const season = rawSeason && /^\d{4}-\d{2}$/.test(rawSeason) ? rawSeason : "";

  return new ImageResponse(
    <SeasonPicksArt
      username={username}
      season={season}
      picks={{ champion: team("champion"), east: team("east_finalist"), west: team("west_finalist"), best: team("best_record") }}
    />,
    { width: 1080, height: 1080, fonts, headers: { "Cache-Control": "public, max-age=300" } }
  );
}
