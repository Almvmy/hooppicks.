import { ImageResponse } from "next/og";
import { TicketArt } from "@/components/share-card-art";
import { fetchSharedTicket } from "@/lib/share";

export const alt = "Ticket HoopPicks";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default async function Image({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const ticket = await fetchSharedTicket(id);
  if (!ticket) {
    return new ImageResponse(
      <div style={{ width: "100%", height: "100%", display: "flex", alignItems: "center", justifyContent: "center", background: "#0B1120", color: "white", fontSize: 64 }}>
        HoopPicks
      </div>,
      size
    );
  }
  return new ImageResponse(<TicketArt ticket={ticket} />, size);
}
