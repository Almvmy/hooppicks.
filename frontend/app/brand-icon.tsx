import { LogoMark } from "@/app/logo-mark";
import { LOGO_FULL } from "@/app/logo-geometry";

// Artwork de l'icône d'app : tuile Nuit pleine, avec le tir qui entre dans
// le panier. Partagé entre l'icône Apple et les icônes PNG du manifest pour
// qu'elles ne divergent jamais. Même dessin que LogoSymbol (l'onglet, icon.svg, montre le vrai logo sans fond).
// Le symbole occupe 60 % du carré : il reste dans la zone sûre « maskable »
// (cercle de 80 %) quand Android découpe l'icône en rond ou en goutte.
const [, , vbW, vbH] = LOGO_FULL.viewBox.split(" ").map(Number);

export function BrandIconArtwork({ size }: { size: number }) {
  const width = Math.round(size * 0.6);
  const height = Math.round((width * vbH) / vbW);
  return (
    <div
      style={{
        width: "100%",
        height: "100%",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "#0B1120",
      }}
    >
      <LogoMark variant="full" rim="#F1F5F9" arc="#00E5FF" width={width} height={height} />
    </div>
  );
}
