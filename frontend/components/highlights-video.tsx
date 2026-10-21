"use client";

import { useState } from "react";
import type { Match } from "@/lib/types";
import { cn } from "@/lib/utils";

type Source = { key: string; label: string; videoId: string; provider: "youtube" | "espn"; caption: string };

/** Vidéos d'un match terminé : résumé neutre (NBA, ESPN) puis celui de chaque équipe, s'ils existent. */
export function highlightSources(match: Match): Source[] {
  const sources: Source[] = [];
  if (match.highlightsVideoId) {
    const espn = match.highlightsProvider === "espn";
    sources.push({
      key: "main",
      label: "Résumé",
      videoId: match.highlightsVideoId,
      provider: espn ? "espn" : "youtube",
      caption: espn ? "Résumé ESPN" : "Vidéo officielle de la NBA",
    });
  }
  // Chaque équipe filme surtout son camp : proposées à côté, jamais à la place.
  for (const [team, videoId] of [
    [match.homeTeam, match.homeHighlightsVideoId],
    [match.awayTeam, match.awayHighlightsVideoId],
  ] as const) {
    if (videoId) {
      sources.push({
        key: team.abbreviation,
        label: `Vu par les ${team.name}`,
        videoId,
        provider: "youtube",
        caption: `Chaîne officielle des ${team.name}`,
      });
    }
  }
  return sources;
}

/**
 * Résumé vidéo dans le lecteur prévu pour être intégré : YouTube (domaine
 * youtube-nocookie, pas de cookie tant que la vidéo n'est pas lancée) ou le
 * lecteur « syndicated » d'ESPN. Jamais de diffusion des matchs eux-mêmes
 * (droits NBA).
 */
export function HighlightsVideo({ match }: { match: Match }) {
  const sources = highlightSources(match);
  const [selected, setSelected] = useState(sources[0]?.key);
  const source = sources.find((s) => s.key === selected) ?? sources[0];
  if (!source) return null;

  const id = encodeURIComponent(source.videoId);
  const src =
    source.provider === "espn"
      ? `https://www.espn.com/watch/syndicatedplayer/_/id/${id}/endcard/false`
      : `https://www.youtube-nocookie.com/embed/${id}?rel=0`;
  return (
    // Largeur bornée par la hauteur de l'écran (barre du haut, onglets et
    // barres du navigateur déduits) : en pleine largeur sur ordinateur, la
    // vidéo dépassait en bas et ses contrôles sortaient de l'écran.
    <div className="mx-auto flex w-full max-w-[calc((100dvh_-_15rem)*16/9)] flex-col gap-2">
      {sources.length > 1 && (
        <div className="glass-scroll edge-scroll flex gap-2 overflow-x-auto pb-0.5" role="group" aria-label="Choisir la vidéo">
          {sources.map((s) => (
            <button
              key={s.key}
              type="button"
              aria-pressed={s.key === source.key}
              onClick={() => setSelected(s.key)}
              className={cn(
                "shrink-0 rounded-full px-3 py-1.5 text-sm transition-colors",
                s.key === source.key ? "glass-accent" : "glass-inset-quiet text-muted-foreground hover:text-foreground"
              )}
            >
              {s.label}
            </button>
          ))}
        </div>
      )}
      <div className="glass relative aspect-video w-full overflow-hidden rounded-2xl">
        <iframe
          key={source.key}
          src={src}
          title={`${match.awayTeam.name} à ${match.homeTeam.name} : ${source.label.toLowerCase()}`}
          loading="lazy"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; fullscreen; gyroscope; picture-in-picture; web-share"
          referrerPolicy="strict-origin-when-cross-origin"
          allowFullScreen
          className="absolute inset-0 h-full w-full"
        />
      </div>
      <p className="text-xs text-muted-foreground">{source.caption}</p>
    </div>
  );
}
