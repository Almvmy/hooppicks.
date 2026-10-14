// Polices des images de partage (ImageResponse / Satori). Satori ne lit pas le
// woff2 de l'app : on charge des woff statiques, sous-ensemble latin.
//  - Barlow Condensed : titres, gros chiffres, étiquettes en capitales (look
//    « maillot / affiche de match »)
//  - Barlow : textes courants
// Licence SIL OFL (paquets @fontsource/barlow et @fontsource/barlow-condensed).

import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";

type ShareFont = { name: string; data: ArrayBuffer; weight: 500 | 700 | 800 | 900; style: "normal" | "italic" };

// Node (pas Edge) : lecture du fichier que le bundler a copié à côté du serveur.
const load = async (url: URL): Promise<ArrayBuffer> => {
  const buf = await readFile(fileURLToPath(url));
  return buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength) as ArrayBuffer;
};

let cache: Promise<ShareFont[] | undefined> | null = null;

/** `undefined` si la lecture échoue : ImageResponse reprend alors sa police par défaut (une liste vide ferait planter le rendu). */
export function shareFonts(): Promise<ShareFont[] | undefined> {
  cache ??= Promise.all([
    load(new URL("../app/fonts/share/barlow-condensed-latin-700-normal.woff", import.meta.url)),
    load(new URL("../app/fonts/share/barlow-condensed-latin-800-normal.woff", import.meta.url)),
    load(new URL("../app/fonts/share/barlow-condensed-latin-900-normal.woff", import.meta.url)),
    load(new URL("../app/fonts/share/barlow-condensed-latin-900-italic.woff", import.meta.url)),
    load(new URL("../app/fonts/share/barlow-latin-500-normal.woff", import.meta.url)),
    load(new URL("../app/fonts/share/barlow-latin-700-normal.woff", import.meta.url)),
  ])
    .then(([c7, c8, c9, c9i, b5, b7]): ShareFont[] => [
      { name: "Barlow Condensed", data: c7, weight: 700, style: "normal" },
      { name: "Barlow Condensed", data: c8, weight: 800, style: "normal" },
      { name: "Barlow Condensed", data: c9, weight: 900, style: "normal" },
      { name: "Barlow Condensed", data: c9i, weight: 900, style: "italic" },
      { name: "Barlow", data: b5, weight: 500, style: "normal" },
      { name: "Barlow", data: b7, weight: 700, style: "normal" },
    ])
    .catch((e) => {
      console.error("shareFonts : polices illisibles, police par défaut utilisée", e);
      // Sans polices, ImageResponse retombe sur sa police par défaut : l'image sort quand même.
      cache = null;
      return undefined;
    });
  return cache;
}
