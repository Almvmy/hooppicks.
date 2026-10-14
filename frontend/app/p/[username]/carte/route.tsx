import { ImageResponse } from "next/og";
import { RecapArt } from "@/components/share-card-art";
import { fetchSharedPlayer } from "@/lib/share";
import { shareFonts } from "@/lib/share-fonts";
import { seasonLabel } from "@/lib/utils";

// Image de la carte de partage (page Profil), 1080×1080 : envoyée en photo
// par le bouton « Partager l'image ».
export async function GET(_request: Request, { params }: { params: Promise<{ username: string }> }) {
  const { username } = await params;
  const [player, fonts] = await Promise.all([fetchSharedPlayer(decodeURIComponent(username), true), shareFonts()]);
  if (!player) return new Response("Joueur introuvable", { status: 404 });
  return new ImageResponse(<RecapArt player={player} seasonLabel={`Saison ${seasonLabel()}`} />, {
    width: 1080,
    height: 1080,
    fonts,
    headers: { "Cache-Control": "no-store" },
  });
}
