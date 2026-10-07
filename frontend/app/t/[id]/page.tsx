import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ShareLanding } from "@/components/share-landing";
import { fetchSharedTicket, plainNumber, signedPlain } from "@/lib/share";

type Props = { params: Promise<{ id: string }> };

function headline(t: NonNullable<Awaited<ReturnType<typeof fetchSharedTicket>>>) {
  if (t.status === "won") return `Ticket gagnant de @${t.username} : ${signedPlain(t.potentialPayout - t.stake)} pts`;
  if (t.status === "lost") return `Ticket de @${t.username}`;
  return `Ticket remboursé de @${t.username}`;
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const ticket = await fetchSharedTicket((await params).id);
  if (!ticket) return { title: "Ticket introuvable" };
  const title = headline(ticket);
  const description = `${ticket.selections.map((s) => s.label).join(" · ")} · cote ${ticket.totalOdds.toFixed(2).replace(".", ",")}`;
  return {
    title,
    description,
    openGraph: { title, description },
    twitter: { card: "summary_large_image", title, description },
  };
}

export default async function SharedTicketPage({ params }: Props) {
  const { id } = await params;
  const ticket = await fetchSharedTicket(id);
  if (!ticket) notFound();

  const net = ticket.status === "won" ? ticket.potentialPayout - ticket.stake : ticket.status === "lost" ? -ticket.stake : 0;
  return (
    <ShareLanding appHref="/bets">
      <div className="glass flex flex-col gap-4 rounded-2xl p-5">
        <div className="flex items-baseline justify-between gap-3">
          <p className="font-heading text-xl font-bold">{headline(ticket)}</p>
        </div>
        <div className="flex flex-col divide-y divide-tint/10">
          {ticket.selections.map((s, i) => (
            <div key={i} className="flex items-center justify-between gap-3 py-2">
              <div className="min-w-0">
                <p className="font-semibold">{s.label}</p>
                {s.matchLabel && <p className="truncate text-xs text-muted-foreground">{s.matchLabel}</p>}
              </div>
              <span className="font-mono text-sm">{s.odds.toFixed(2).replace(".", ",")}</span>
            </div>
          ))}
        </div>
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>
            Mise {plainNumber(ticket.stake)} pts · cote {ticket.totalOdds.toFixed(2).replace(".", ",")}
          </span>
          <span className={net > 0 ? "font-mono font-bold text-success" : net < 0 ? "font-mono font-bold text-destructive" : "font-mono"}>
            {ticket.status === "void" ? "mise rendue" : `${signedPlain(net)} pts`}
          </span>
        </div>
      </div>
    </ShareLanding>
  );
}
