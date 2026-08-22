type LogoSymbolProps = {
  className?: string;
  /** Même famille dans les deux cas — un disque orange plein (pas un
   *  contour vide : un vrai ballon est une sphère colorée, pas un anneau) et
   *  un check en creux. "full" (36-48px+ : header d'auth, page d'accueil)
   *  ajoute deux coutures supplémentaires (4 au total) que la place permet.
   *  "compact" (≤28px : sidebar, topbar mobile, nav) reste à 2 coutures —
   *  au-delà, à cette taille, ça devient du bruit plutôt que de la texture. */
  variant?: "full" | "compact";
};

export const LogoSymbol = ({ className = "w-10 h-10", variant = "full" }: LogoSymbolProps) => (
  <svg viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg" className={className}>
    <circle cx="50" cy="50" r="42" fill="#FF7A1A" />
    <path d="M10,42 Q50,55 90,42" stroke="#0B1120" strokeWidth="3.5" opacity="0.5" />
    <path d="M50,8 Q37,50 50,92" stroke="#0B1120" strokeWidth="3.5" opacity="0.5" />
    {variant === "full" && (
      <>
        <path d="M25,14 C38,28 38,72 25,86" stroke="#0B1120" strokeWidth="3" opacity="0.4" />
        <path d="M75,14 C62,28 62,72 75,86" stroke="#0B1120" strokeWidth="3" opacity="0.4" />
      </>
    )}
    {/* Forme fermée (remplissage), pas un trait épais : un trait aussi large
        que l'angle du check comble tout l'intérieur du "V" et se lit comme
        un triangle plein plutôt qu'un check. */}
    <path
      d="M21.1,49.6 L37.3,65.7 L44.9,65.5 L79.9,25.5 L72,18.6 L37.1,58.5 L44.7,58.3 L28.6,42.1 Z"
      fill="#F1F5F9"
    />
  </svg>
);
