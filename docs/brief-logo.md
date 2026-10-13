# Brief logo HoopPicks

## Le produit
App française de pronostics NBA en **points virtuels** (pas d'argent réel) : on parie sur les matchs, on grimpe au classement, on affronte ses potes dans des ligues, on débloque des badges. On y joue pour le jeu et la gloriole, pas pour gagner de l'argent.

## Ce qu'on veut
- **Type** : un **symbole + le nom**. Le symbole doit vivre seul (icône d'app, favicon) et se combiner avec le mot « HoopPicks ».
- **Ton** : **minimaliste tech**. Ultra simple, géométrique, dans l'esprit des icônes d'apps modernes. Confiant et sportif, jamais enfantin.
- **Idée centrale** : ouverte. Le symbole doit évoquer le **basket** et le **pronostic** (faire un choix, viser juste). Directions à explorer :
  - ballon qui fusionne avec un signe de choix (coche, flèche, cible) ;
  - arceau et filet, « ça rentre » ;
  - lignes du terrain qui dessinent un H ou un P ;
  - monogramme HP retravaillé en signe ;
  - pistes libres.
- **Couleurs** : celles de l'app, sans en ajouter.
  - Orange de marque `#FF7A1A`
  - Marine `#121A2E` / `#0B1120`
  - Thème clair : sable `#ECE3D3`, crème `#F6F0E5`, orange brûlé `#A13C0A` pour le texte

## Contraintes
- **Lisible à 16 px** (onglet de navigateur) : formes simples, pas de détails fins.
- **Icône d'app carrée** (écran d'accueil iPhone et Android, zone de sécurité « maskable »).
- **Une seule couleur** possible (impression, notifications système).
- **Sombre et clair** : doit tenir sur marine comme sur sable.
- **Deux déclinaisons** : symbole seul / symbole + « HoopPicks » (le « Picks » est aujourd'hui en orange dans le nom).

## À éviter
- Tout ce qui rappelle le **logo NBA** (silhouette de joueur, rectangle vertical rouge et bleu) : marque déposée.
- Les **logos d'équipes** NBA.
- Les codes des **sites de paris d'argent** : jetons, billets, dés, couleurs casino.
- Les dégradés et effets indispensables à la lecture : le logo doit marcher à plat.

## Existant
Ancien monogramme « HP » posé sur un ballon, jugé pas assez fort : remplacé en octobre 2026 par **le tir qui entre dans le panier** (ballon dans la jante, filet, tir en pointillés). Kit complet (SVG, PNG, favicon, guide d'usage) dans `docs/logo/`. Dans le code : `frontend/app/logo-geometry.ts` (dessin), `logo-mark.tsx` (composant), `LogoSymbol.tsx` (interface), `brand-icon.tsx` + `apple-icon.tsx` + `pwa-icon` (icônes d'app), `icon.svg` et `favicon.ico`.
Trois déclinaisons du symbole : « mid » (36 à 72 px, avec le tir en pointillés : barre du haut, navigation d'accueil, connexion), « full » (≥ 72 px, accueil) et « small » (≤ 32 px, sans pointillés).

## Avant d'aller plus loin
Vérifier que le nom « HoopPicks » n'est pas déjà déposé (INPI, EUIPO) : une app au nom très proche existe.
