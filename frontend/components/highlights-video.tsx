/**
 * Résumé vidéo officiel dans le lecteur prévu pour être intégré : YouTube
 * (domaine youtube-nocookie, pas de cookie tant que la vidéo n'est pas
 * lancée) ou le lecteur « syndicated » d'ESPN. Jamais de diffusion des
 * matchs eux-mêmes (droits NBA).
 */
export function HighlightsVideo({
  videoId,
  provider,
  title,
}: {
  videoId: string;
  provider: "youtube" | "espn";
  title: string;
}) {
  const id = encodeURIComponent(videoId);
  const src =
    provider === "espn"
      ? `https://www.espn.com/watch/syndicatedplayer/_/id/${id}/endcard/false`
      : `https://www.youtube-nocookie.com/embed/${id}?rel=0`;
  return (
    // Largeur bornée par la hauteur de l'écran (barre du haut, onglets et
    // barres du navigateur déduits) : en pleine largeur sur ordinateur, la
    // vidéo dépassait en bas et ses contrôles sortaient de l'écran.
    <div className="mx-auto flex w-full max-w-[calc((100dvh_-_15rem)*16/9)] flex-col gap-2">
      <div className="glass relative aspect-video w-full overflow-hidden rounded-2xl">
        <iframe
          src={src}
          title={title}
          loading="lazy"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; fullscreen; gyroscope; picture-in-picture; web-share"
          referrerPolicy="strict-origin-when-cross-origin"
          allowFullScreen
          className="absolute inset-0 h-full w-full"
        />
      </div>
      <p className="text-xs text-muted-foreground">{provider === "espn" ? "Résumé ESPN" : "Vidéo officielle de la NBA"}</p>
    </div>
  );
}
