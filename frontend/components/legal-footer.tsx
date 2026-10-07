import Link from "next/link";

/**
 * Pied de page des pages publiques (accueil, confidentialité). Pas dans
 * l'espace connecté : sur mobile la barre du bas occupe déjà cette place,
 * les mêmes liens y sont dans le menu du profil.
 */
export function LegalFooter({ children }: { children?: React.ReactNode }) {
  return (
    <footer className="glass-hairline-t flex flex-col items-center gap-4 px-4 py-8 text-center text-xs text-muted-foreground">
      {children}
      <nav aria-label="Liens utiles" className="flex flex-wrap justify-center gap-x-5 gap-y-2 text-sm">
        <Link href="/about" className="hover:text-foreground">
          À propos
        </Link>
        <Link href="/confidentialite" className="hover:text-foreground">
          Confidentialité
        </Link>
        <a href="mailto:almamyksg@gmail.com" className="hover:text-foreground">
          Contact
        </a>
      </nav>
      <p>HoopPicks · pronostics NBA en points virtuels, aucun argent réel en jeu.</p>
      <p>Projet indépendant, non affilié à la NBA, à ses équipes ni à ESPN.</p>
    </footer>
  );
}
