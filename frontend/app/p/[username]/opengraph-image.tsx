import { ImageResponse } from "next/og";
import { PlayerArt } from "@/components/share-card-art";
import { fetchSharedPlayer } from "@/lib/share";

export const alt = "Joueur HoopPicks";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default async function Image({ params }: { params: Promise<{ username: string }> }) {
  const { username } = await params;
  const player = await fetchSharedPlayer(decodeURIComponent(username));
  if (!player) {
    return new ImageResponse(
      <div style={{ width: "100%", height: "100%", display: "flex", alignItems: "center", justifyContent: "center", background: "#0B1120", color: "white", fontSize: 64 }}>
        HoopPicks
      </div>,
      size
    );
  }
  return new ImageResponse(<PlayerArt player={player} />, size);
}
