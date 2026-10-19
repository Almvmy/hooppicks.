import { ImageResponse } from "next/og";
import { ChampionArt } from "@/components/share-card-art";
import { fetchSharedChampions, fetchSharedPhaseChampions } from "@/lib/share";
import { shareFonts } from "@/lib/share-fonts";
import { weekLabel } from "@/lib/utils";

// Image du champion de la semaine passée (ou, avec ?phase=cup|playoffs, de la
// Coupe NBA ou des playoffs de la saison), 1080×1080 : envoyée en photo par
// « Partager l'image » sur le bandeau du classement. Route publique, comme le classement.
export async function GET(request: Request) {
  const raw = new URL(request.url).searchParams.get("phase");
  const phase = raw === "cup" || raw === "playoffs" ? raw : null;
  const fonts = shareFonts();

  if (phase) {
    const data = await fetchSharedPhaseChampions(phase);
    if (!data || data.champions.length === 0) return new Response("Pas encore de champion", { status: 404 });
    const of = phase === "cup" ? "de la Coupe NBA" : "des playoffs";
    return new ImageResponse(<ChampionArt champions={data.champions} weekLabel={`Saison ${data.season}`} of={of} />, {
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
