import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ShareLanding } from "@/components/share-landing";
import { fetchSharedPlayer, signedPlain } from "@/lib/share";

type Props = { params: Promise<{ username: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const player = await fetchSharedPlayer(decodeURIComponent((await params).username));
  if (!player) return { title: "Joueur introuvable" };
  const title = `@${player.username} sur HoopPicks`;
  const description = player.seasonRank
    ? `#${player.seasonRank} sur ${player.seasonPlayers} cette saison · ${signedPlain(player.seasonPoints)} pts. Viens le défier !`
    : "Pronostics NBA entre amis. Viens le défier !";
  return {
    title,
    description,
    openGraph: { title, description },
    twitter: { card: "summary_large_image", title, description },
  };
}

export default async function SharedPlayerPage({ params }: Props) {
  const username = decodeURIComponent((await params).username);
  const player = await fetchSharedPlayer(username);
  if (!player) notFound();

  const stats = [
    { label: "Classement", value: player.seasonRank ? `#${player.seasonRank}` : "—", hint: player.seasonRank ? `sur ${player.seasonPlayers}` : "pas encore classé" },
    { label: "Points", value: signedPlain(player.seasonPoints), hint: "cette saison" },
    { label: "Réussite", value: player.settledTickets ? `${player.winRate}%` : "—", hint: `${player.settledTickets} tickets réglés` },
    { label: "Record", value: `${player.bestStreak}`, hint: "gagnés d'affilée" },
  ];
  return (
    <ShareLanding appHref={`/u/${encodeURIComponent(player.username)}`}>
      <div className="glass flex flex-col gap-4 rounded-2xl p-5">
        <div>
          <p className="font-heading text-2xl font-bold">@{player.username}</p>
          {player.favoriteTeam && <p className="text-sm text-muted-foreground">Supporter des {player.favoriteTeam}</p>}
        </div>
        <div className="grid grid-cols-2 gap-3">
          {stats.map((s) => (
            <div key={s.label} className="glass-inset-quiet rounded-xl p-3">
              <p className="text-[11px] uppercase tracking-wide text-muted-foreground">{s.label}</p>
              <p className="font-heading text-2xl font-bold">{s.value}</p>
              <p className="text-xs text-muted-foreground">{s.hint}</p>
            </div>
          ))}
        </div>
      </div>
    </ShareLanding>
  );
}
