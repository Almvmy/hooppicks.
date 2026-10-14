import { ImageResponse } from "next/og";
import { NotFoundArt, PlayerArt } from "@/components/share-card-art";
import { shareFonts } from "@/lib/share-fonts";
import { fetchSharedPlayer } from "@/lib/share";

export const alt = "Joueur HoopPicks";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default async function Image({ params }: { params: Promise<{ username: string }> }) {
  const { username } = await params;
  const [player, fonts] = await Promise.all([fetchSharedPlayer(decodeURIComponent(username)), shareFonts()]);
  if (!player) {
    return new ImageResponse(<NotFoundArt title="Joueur introuvable" hint="Ce profil n'existe pas (ou plus)." />, { ...size, fonts });
  }
  return new ImageResponse(<PlayerArt player={player} />, { ...size, fonts });
}
