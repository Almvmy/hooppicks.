import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "HoopPicks — Pronostics NBA",
    short_name: "HoopPicks",
    description:
      "Pronostique sur chaque match de la saison NBA en points virtuels, grimpe dans le classement, aucun argent réel en jeu.",
    start_url: "/",
    display: "standalone",
    background_color: "#0B1120",
    theme_color: "#0B1120",
    icons: [
      {
        src: "/icon.svg",
        sizes: "any",
        type: "image/svg+xml",
      },
    ],
  };
}
