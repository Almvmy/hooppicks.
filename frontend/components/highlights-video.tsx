/**
 * Résumé vidéo officiel (chaîne YouTube de la NBA) dans le lecteur YouTube.
 * Domaine youtube-nocookie : pas de cookie YouTube tant que la vidéo n'est
 * pas lancée. Jamais de diffusion des matchs eux-mêmes (droits NBA).
 */
export function HighlightsVideo({ videoId, title }: { videoId: string; title: string }) {
  return (
    <div className="flex flex-col gap-2">
      <div className="glass relative aspect-video w-full overflow-hidden rounded-2xl">
        <iframe
          src={`https://www.youtube-nocookie.com/embed/${encodeURIComponent(videoId)}?rel=0`}
          title={title}
          loading="lazy"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
          referrerPolicy="strict-origin-when-cross-origin"
          allowFullScreen
          className="absolute inset-0 h-full w-full"
        />
      </div>
      <p className="text-xs text-muted-foreground">Vidéo officielle de la NBA</p>
    </div>
  );
}
