import { ImageResponse } from "next/og";
import { NotFoundArt, TicketArt } from "@/components/share-card-art";
import { shareFonts } from "@/lib/share-fonts";
import { fetchSharedTicket } from "@/lib/share";

export const alt = "Ticket HoopPicks";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default async function Image({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [ticket, fonts] = await Promise.all([fetchSharedTicket(id), shareFonts()]);
  if (!ticket) {
    return new ImageResponse(<NotFoundArt title="Ticket introuvable" hint="Ce lien n'est plus valide." />, { ...size, fonts });
  }
  return new ImageResponse(<TicketArt ticket={ticket} />, { ...size, fonts });
}
