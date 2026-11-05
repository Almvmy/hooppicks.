import { ImageResponse } from "next/og";
import { WeekRecapArt } from "@/components/share-card-art";
import { fetchSharedWeek } from "@/lib/share";
import { shareFonts } from "@/lib/share-fonts";
import { weekLabel } from "@/lib/utils";

// La semaine passée d'un joueur en image, 1080×1080 : envoyée en photo depuis
// la story du lundi. Lue en base (jamais dans l'adresse) : un score ne se falsifie pas.
export async function GET(_request: Request, { params }: { params: Promise<{ username: string }> }) {
  const { username } = await params;
  const [week, fonts] = await Promise.all([fetchSharedWeek(decodeURIComponent(username)), shareFonts()]);
  if (!week) return new Response("Pas de semaine à montrer", { status: 404 });
  return new ImageResponse(<WeekRecapArt week={week} label={weekLabel(week.week)} />, {
    width: 1080,
    height: 1080,
    fonts,
    headers: { "Cache-Control": "no-store" },
  });
}
