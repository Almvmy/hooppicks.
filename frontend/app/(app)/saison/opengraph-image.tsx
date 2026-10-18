import { ImageResponse } from "next/og";
import { SeasonTeaserArt } from "@/components/share-card-art";
import { fetchSharedSeasonPicks } from "@/lib/share";
import { shareFonts } from "@/lib/share-fonts";

export const alt = "Pronostics de saison HoopPicks";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default async function Image() {
  const [overview, fonts] = await Promise.all([fetchSharedSeasonPicks(), shareFonts()]);
  return new ImageResponse(
    <SeasonTeaserArt season={overview?.season ?? ""} participants={overview?.participants ?? 0} revealed={!!overview?.locked} />,
    { ...size, fonts }
  );
}
