import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "HoopPicks - Pronostics NBA",
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
      { src: "/pwa-icon/192", sizes: "192x192", type: "image/png" },
      { src: "/pwa-icon/512", sizes: "512x512", type: "image/png" },
      // Le tile orange plein remplit déjà tout le carré, et le monogramme
      // tient dans la zone sûre centrale (80 %) : utilisable tel quel quand
      // Android découpe l'icône en cercle ou en goutte.
      { src: "/pwa-icon/512", sizes: "512x512", type: "image/png", purpose: "maskable" },
    ],
  };
}
