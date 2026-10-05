import { ImageResponse } from "next/og";
import { BrandIconArtwork } from "@/app/brand-icon";

// Android/Chrome n'acceptent de proposer l'installation qu'avec des icônes
// raster 192 et 512 px dans le manifest : l'icon.svg seule ne suffit pas.
const ALLOWED_SIZES = new Set([192, 512]);

export async function GET(_request: Request, { params }: { params: Promise<{ size: string }> }) {
  const size = Number((await params).size);
  if (!ALLOWED_SIZES.has(size)) {
    return new Response("Not found", { status: 404 });
  }
  return new ImageResponse(<BrandIconArtwork size={size} />, {
    width: size,
    height: size,
    headers: { "Cache-Control": "public, max-age=86400, immutable" },
  });
}
