import { LogoMark } from "@/app/logo-mark";

/** Partagée avec l'image générée de la carte (share-card-art), pour qu'elles disent la même chose. */
export function headlineFor(streak: number, weeklyDelta: number): string {
  if (streak >= 5) return `${streak} pronostics gagnés d'affilée.\nNuit parfaite.`;
  if (streak >= 3) return `${streak} pronostics gagnés d'affilée.\nÇa chauffe.`;
  if (weeklyDelta > 0) return "Une bonne semaine.\nEncore un peu de sauce.";
  if (weeklyDelta < 0) return "Semaine compliquée.\nOn revient plus fort.";
  return "Nouvelle semaine.\nNouveaux pronostics.";
}

export function WeeklyRecapCard({
  username,
  weeklyDelta,
  rank,
  totalPlayers,
  winRate,
  streak,
  seasonLabel,
}: {
  username: string;
  weeklyDelta: number;
  rank?: number;
  totalPlayers?: number;
  winRate: number;
  streak: number;
  seasonLabel: string;
}) {
  const [line1, line2] = headlineFor(streak, weeklyDelta).split("\n");

  return (
    // island : la carte à partager garde son look Nuit (marine) en thème
    // clair aussi, pour qu'une capture partagée ressemble toujours à HoopPicks.
    <div className="relative w-full max-w-[420px] overflow-hidden rounded-[14px] border border-white/10 bg-[#0B1120] shadow-[0_20px_48px_rgba(0,0,0,.55),0_0_0_1px_rgba(255,255,255,.05)]">
      {/* Accent gauche orange */}
      <div className="absolute bottom-0 left-0 top-0 w-1 rounded-l-[14px] bg-primary" />

      {/* Filigrane logo full en fond */}
      <div className="pointer-events-none absolute -bottom-6 -right-7 opacity-[0.07]">
        <LogoMark
          variant="full"
          width={200}
          height={200}
          rim="#F4EFE4"
          arc="#F4EFE4"
          ball="#F4EFE4"
          seam="#0B1120"
        />
      </div>

      <div className="relative px-[22px] pb-[18px] pt-5">
        {/* Header */}
        <div className="mb-4 flex items-center justify-between">
          <div className="flex items-center gap-[9px]">
            <LogoMark
              variant="medium"
              width={34}
              height={34}
              className="shrink-0 text-foreground"
            />
            <span className="font-condensed text-[16px] font-extrabold leading-none tracking-[.02em] text-foreground">
              Hoop<span className="text-primary">Picks</span>
            </span>
          </div>
          <span className="font-condensed text-[11px] font-bold uppercase tracking-[.13em] text-[#334155]">
            {seasonLabel}
          </span>
        </div>

        {/* Divider */}
        <div className="mb-[14px] h-px bg-white/[0.07]" />

        {/* Headline condensed italic */}
        <div className="mb-4 leading-[.95]">
          <span className="block font-condensed text-[32px] font-black italic uppercase tracking-[.01em] text-foreground">
            {line1}
          </span>
          <span className="block font-condensed text-[32px] font-black italic uppercase tracking-[.01em] text-primary">
            {line2}
          </span>
        </div>

        {/* Bandelette papier pour les stats */}
        <div className="flex items-stretch rounded-[8px] bg-[#F4EFE4] px-4 py-[13px]">
          {/* Points */}
          <div className="flex flex-1 flex-col items-start">
            <span
              className={`font-condensed text-[28px] font-black italic leading-none ${
                weeklyDelta >= 0 ? "text-[#15803d]" : "text-[#b91c1c]"
              }`}
            >
              {weeklyDelta > 0 ? "+" : ""}
              {weeklyDelta.toLocaleString("fr-FR")}
            </span>
            <span className="mt-1 font-condensed text-[10px] font-bold uppercase tracking-[.10em] text-[#6B6558]">
              Points
            </span>
          </div>

          <div className="w-px self-stretch bg-[#D9D2C1]" />

          {/* Classement */}
          <div className="flex flex-1 flex-col items-center">
            <span className="font-condensed text-[28px] font-black italic leading-none text-[#0B1120]">
              {rank ? `#${rank}` : "–"}
            </span>
            <span className="mt-1 text-center font-condensed text-[10px] font-bold uppercase tracking-[.10em] text-[#6B6558]">
              Classement{totalPlayers ? ` / ${totalPlayers}` : ""}
            </span>
          </div>

          <div className="w-px self-stretch bg-[#D9D2C1]" />

          {/* Réussite */}
          <div className="flex flex-1 flex-col items-end">
            <span className="font-condensed text-[28px] font-black italic leading-none text-[#0B1120]">
              {winRate}%
            </span>
            <span className="mt-1 font-condensed text-[10px] font-bold uppercase tracking-[.10em] text-[#6B6558]">
              Réussite
            </span>
          </div>
        </div>

        {/* Username */}
        <div className="mt-[10px] flex justify-end">
          <span className="font-condensed text-[12px] font-bold tracking-[.04em] text-[#334155]">
            @{username}
          </span>
        </div>
      </div>
    </div>
  );
}
