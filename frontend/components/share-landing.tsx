import Link from "next/link";
import { LegalFooter } from "@/components/legal-footer";

/**
 * Habillage des pages ouvertes depuis un lien partagé : la personne n'a
 * souvent pas de compte, la page doit donc montrer le contenu ET donner
 * envie de rejoindre, sans l'interface de l'app autour.
 */
export function ShareLanding({ children, appHref }: { children: React.ReactNode; appHref: string }) {
  return (
    <div className="app-field flex min-h-screen flex-col">
      <main className="mx-auto flex w-full max-w-xl flex-1 flex-col gap-5 px-4 py-10">
        <Link href="/" className="font-heading text-lg font-bold">
          HoopPicks
        </Link>
        {children}
        <div className="glass flex flex-col gap-3 rounded-2xl p-5 text-center">
          <p className="font-heading text-lg font-bold">Pronostique la NBA entre amis</p>
          <p className="text-sm text-muted-foreground">
            Chaque semaine, tout le monde repart avec 1 000 points virtuels. Aucun argent réel : juste le classement, les
            ligues et les duels.
          </p>
          <div className="flex flex-wrap justify-center gap-2">
            <Link href="/register" className="rounded-full bg-[var(--brand)] px-5 py-2.5 text-sm font-bold text-[var(--jersey)]">
              Créer mon compte
            </Link>
            <Link href={appHref} className="glass-inset rounded-full px-5 py-2.5 text-sm font-semibold">
              Déjà inscrit ? Ouvrir
            </Link>
          </div>
        </div>
      </main>
      <LegalFooter />
    </div>
  );
}
