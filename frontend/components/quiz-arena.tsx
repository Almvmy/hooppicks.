import { Button } from "@/components/ui/button";

const STANDS = [
  { y: 96, r: 2.4, gap: 15 },
  { y: 110, r: 2.8, gap: 17 },
  { y: 126, r: 3.2, gap: 19 },
  { y: 144, r: 3.6, gap: 22 },
];

/** Une salle dessinée, sans photo ni logo : projecteurs, tribunes, panier, parquet. */
function ArenaScene() {
  return (
    <svg aria-hidden viewBox="0 0 672 440" preserveAspectRatio="xMidYMid slice" className="absolute inset-0 h-full w-full">
      <defs>
        <linearGradient id="arena-beam" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#FFF4E0" stopOpacity="0.2" />
          <stop offset="1" stopColor="#FFF4E0" stopOpacity="0" />
        </linearGradient>
        <linearGradient id="arena-floor" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#6B4320" />
          <stop offset="1" stopColor="#2E1C0D" />
        </linearGradient>
        <radialGradient id="arena-pool" cx="0.5" cy="0.5" r="0.5">
          <stop offset="0" stopColor="#FFB877" stopOpacity="0.32" />
          <stop offset="1" stopColor="#FFB877" stopOpacity="0" />
        </radialGradient>
      </defs>

      <rect width="672" height="440" fill="#0B1120" />
      {STANDS.flatMap((row) =>
        Array.from({ length: Math.floor(672 / row.gap) + 1 }, (_, i) => (
          // Décalage d'une rangée sur l'autre et de quelques sièges allumés : une
          // foule régulière, mais pas une grille. Calculé, pour un rendu stable.
          <circle
            key={`${row.y}-${i}`}
            cx={i * row.gap + (row.y % 2 ? row.gap / 2 : 0)}
            cy={row.y}
            r={row.r}
            fill={(i * 7 + row.y) % 11 === 0 ? "#FF7A1A" : "#94A3B8"}
            opacity={(i * 3 + row.y) % 5 === 0 ? 0.42 : 0.18}
          />
        ))
      )}

      <polygon points="120,0 164,0 340,250 170,250" fill="url(#arena-beam)" />
      <polygon points="508,0 552,0 502,250 332,250" fill="url(#arena-beam)" />
      <polygon points="316,0 356,0 430,250 242,250" fill="url(#arena-beam)" />

      <polygon points="60,178 612,178 780,440 -108,440" fill="url(#arena-floor)" />
      <ellipse cx="336" cy="232" rx="210" ry="58" fill="url(#arena-pool)" />
      <g fill="none" stroke="#F1F5F9" strokeOpacity="0.5" strokeWidth="2.5">
        <polygon points="272,178 400,178 438,262 234,262" />
        <path d="M234 262 A102 30 0 0 0 438 262" />
        <path d="M60 178 H612" />
        <path d="M150 178 C150 268 522 268 522 178" />
      </g>

      <g>
        <rect x="334" y="44" width="4" height="30" fill="#475569" />
        <rect x="294" y="66" width="84" height="54" rx="3" fill="#121A2E" stroke="#F1F5F9" strokeWidth="2.5" />
        <rect x="320" y="84" width="32" height="24" fill="none" stroke="#F1F5F9" strokeWidth="2" />
        <g stroke="#F1F5F9" strokeOpacity="0.8" strokeWidth="1.5" fill="none" strokeLinecap="round">
          <path d="M318 118 L326 148 M328 121 L332 148 M344 121 L340 148 M354 118 L346 148 M326 148 Q336 153 346 148" />
        </g>
        <ellipse cx="336" cy="117" rx="19" ry="4.5" fill="none" stroke="#FF7A1A" strokeWidth="3.5" />
      </g>
    </svg>
  );
}

function Ball() {
  return (
    <svg aria-hidden viewBox="0 0 32 32" className="h-8 w-8">
      <circle cx="16" cy="16" r="15" fill="#FF7A1A" />
      <g fill="none" stroke="#0B1120" strokeWidth="1.6" strokeLinecap="round">
        <circle cx="16" cy="16" r="15" />
        <path d="M16 1v30M1 16h30M6 5.5c5 6 5 15 0 21M26 5.5c-5 6-5 15 0 21" />
      </g>
    </svg>
  );
}

/** L'écran avant de jouer : on entre dans la salle. L'île reste sombre dans le thème clair. */
export function QuizArena({ pending, onPlay }: { pending: boolean; onPlay: () => void }) {
  return (
    <div className="island relative flex min-h-[440px] flex-col items-center justify-end gap-3 overflow-hidden rounded-3xl p-6 text-center text-[#F1F5F9] ring-1 ring-white/10">
      <ArenaScene />
      <div className="relative flex gap-1.5" aria-hidden>
        {Array.from({ length: 5 }, (_, i) => (
          <Ball key={i} />
        ))}
      </div>
      <p className="relative font-heading text-2xl font-bold leading-tight">5 tirs, 15 secondes par tir</p>
      <p className="relative text-sm text-[#CBD5E1]">Un seul essai par jour.</p>
      <Button size="lg" className="relative mt-1 w-full max-w-xs" disabled={pending} onClick={onPlay}>
        Jouer
      </Button>
    </div>
  );
}
