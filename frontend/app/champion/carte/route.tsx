import { ImageResponse } from "next/og";
import { ChampionArt } from "@/components/share-card-art";
import { fetchSharedChampions } from "@/lib/share";
import { shareFonts } from "@/lib/share-fonts";
import { weekLabel } from "@/lib/utils";

// Image du champion de la semaine passée, 1080×1080 : envoyée en photo par
// « Partager l'image » sur le bandeau du classement. Route publique, comme le classement.
export async function GET() {
  const [data, fonts] = await Promise.all([fetchSharedChampions(), shareFonts()]);
  if (!data || data.champions.length === 0) return new Response("Pas de champion pour la semaine passée", { status: 404 });

  return new ImageResponse(<ChampionArt champions={data.champions} weekLabel={weekLabel(data.week)} />, {
    width: 1080,
    height: 1080,
    fonts,
    headers: { "Cache-Control": "no-store" },
  });
}
