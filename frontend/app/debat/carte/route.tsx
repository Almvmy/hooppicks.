import { ImageResponse } from "next/og";
import { DebateArt } from "@/components/share-card-art";
import { fetchSharedDebate } from "@/lib/share";
import { shareFonts } from "@/lib/share-fonts";

// Le résultat d'un débat en image, 1080×1080 : la question et les totaux, lus en base.
export async function GET(request: Request) {
  const jour = new URL(request.url).searchParams.get("jour");
  if (!jour || !/^\d{4}-\d{2}-\d{2}$/.test(jour)) return new Response("Date invalide", { status: 400 });
  const [debate, fonts] = await Promise.all([fetchSharedDebate(jour), shareFonts()]);
  if (!debate) return new Response("Pas de débat à montrer", { status: 404 });
  return new ImageResponse(<DebateArt debate={debate} />, {
    width: 1080,
    height: 1080,
    fonts,
    headers: { "Cache-Control": "no-store" },
  });
}
