# HoopPicks — kit logo

Le symbole : un tir (points cyan) qui entre dans le panier. Le ballon est dans la jante, le filet le retient.
Chaque fichier existe en SVG (vectoriel, à privilégier) ; les PNG sont fournis pour les cas où le SVG n'est pas accepté.

## Quel fichier utiliser

| Besoin | Fichier |
|---|---|
| Logo avec le nom, fond sombre | `svg/hooppicks-lockup-horizontal.svg` |
| Logo avec le nom, fond clair | `svg/hooppicks-lockup-horizontal-on-light.svg` |
| Logo empilé (carré, réseaux, affiches) | `svg/hooppicks-lockup-stacked*.svg` |
| Marque seule (sans nom) | `svg/hooppicks-mark.svg` (sombre), `…-on-light.svg` (clair) |
| Une seule couleur (tampon, gravure, fond orange) | `…-mono-white.svg`, `…-mono-black.svg` |
| Tailles moyennes (36 à 72 px : en-têtes) | `svg/hooppicks-mark-medium*.svg` |
| Très petites tailles (≤ 32 px) | `svg/hooppicks-mark-small*.svg` |
| Icône d'app / store (carré plein) | `svg/hooppicks-app-icon.svg`, `png/app-icon-1024.png` |
| Icône Android « maskable » | `png/icon-maskable-512.png`, `png/icon-maskable-192.png` |
| Apple touch icon | `png/apple-touch-icon-180.png` |
| Favicon | `favicon.ico`, `svg/favicon.svg`, `png/favicon-{16,32,48,64,128}.png` |

Les noms sont écrits en tracés : aucune police à installer.

## Règles d'usage

- **Trois versions de la marque.** Complète (pointillés fins) à partir de 72 px ; moyenne (pointillés plus gros, filet épaissi) de 36 à 72 px, c'est celle des en-têtes ; simplifiée (filet épais, sans pointillés) en dessous de ~32 px, lisible jusqu'à 16 px.
- **Zone de respiration** : au moins la moitié du diamètre du ballon autour du logo.
- **Taille minimale du logo avec nom** : 120 px de large (écran), 25 mm (impression).
- **Couleurs** : Nuit `#0B1120`, Orange `#FF7A1A`, Blanc `#F1F5F9`, Cyan `#00E5FF` (`#0097B2` sur fond clair pour garder le contraste).
- **À ne pas faire** : étirer, changer les couleurs du ballon, mettre le logo sombre sur un fond clair (utiliser les versions « on-light »), ajouter une ombre portée.
- Sur une photo ou un fond chargé, utiliser les versions monochromes.

## Dans l'application (Next.js)

- Remplacer `app/icon.svg` par `svg/favicon.svg` et ajouter `favicon.ico`.
- `apple-touch-icon.png` ← `png/apple-touch-icon-180.png`.
- Manifest PWA : `icon-192.png`, `icon-512.png` (purpose `any`) et `icon-maskable-192/512.png` (purpose `maskable`).
- Couleur de thème : `#0B1120`.

## Polices et licences

Nom : Space Grotesk Bold (licence SIL OFL, libre d'usage, y compris commercial), converti en tracés.
Les tracés du symbole sont originaux ; aucun élément emprunté à une marque existante.
Pas de dépôt de marque effectué : à faire par le propriétaire, avec une recherche d'antériorité.
