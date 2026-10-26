import { ImageResponse } from "next/og";
import { ChampionArt } from "@/components/share-card-art";
import { fetchSharedChampions, fetchSharedPhaseChampions } from "@/lib/share";
import { shareFonts } from "@/lib/share-fonts";
import { monthLabel, weekLabel } from "@/lib/utils";

const OF = { cup: "de la Coupe NBA", playoffs: "des playoffs", general: "HoopPicks", month: "du mois" } as const;

// Image du champion de la semaine passée (ou, avec ?phase=cup|playoffs|general|month,
// de la Coupe NBA, des playoffs, de la saison ou du mois passé), 1080×1080 :
// envoyée en photo par « Partager l'image » sur le bandeau du classement.
// Route publique, comme le classement.
export async function GET(request: Request) {
  const raw = new URL(request.url).searchParams.get("phase");
  const phase = raw !== null && raw in OF ? (raw as keyof typeof OF) : null;
  const fonts = shareFonts();

  if (phase) {
    const data = await fetchSharedPhaseChampions(phase);
    if (!data || data.champions.length === 0) return new Response("Pas encore de champion", { status: 404 });
    const label = phase === "month" ? monthLabel(data.season) : `Saison ${data.season}`;
    return new ImageResponse(<ChampionArt champions={data.champions} weekLabel={label} of={OF[phase]} />, {
      width: 1080,
      height: 1080,
      fonts: await fonts,
      headers: { "Cache-Control": "no-store" },
    });
  }

  const data = await fetchSharedChampions();
  if (!data || data.champions.length === 0) return new Response("Pas de champion pour la semaine passée", { status: 404 });

  return new ImageResponse(<ChampionArt champions={data.champions} weekLabel={weekLabel(data.week)} />, {
    width: 1080,
    height: 1080,
    fonts: await fonts,
    headers: { "Cache-Control": "no-store" },
  });
}
