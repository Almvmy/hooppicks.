import type { Metadata } from "next";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { LegalFooter } from "@/components/legal-footer";

export const metadata: Metadata = {
  title: "Confidentialité",
  description: "Les données que HoopPicks conserve, pourquoi, qui peut les voir et comment les supprimer.",
};

// Page publique (hors espace connecté) : elle doit pouvoir être lue avant
// de créer un compte.
const CONTACT = "almamyksg@gmail.com";

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="glass flex flex-col gap-2 rounded-2xl p-5 text-sm leading-relaxed text-muted-foreground">
      <h2 className="font-heading text-base font-bold text-foreground">{title}</h2>
      {children}
    </section>
  );
}

export default function PrivacyPage() {
  return (
    <div className="app-field min-h-screen">
      <main className="mx-auto flex max-w-2xl flex-col gap-4 px-4 py-10">
        <Link href="/" className="flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="h-4 w-4" />
          Accueil
        </Link>
        <div>
          <h1 className="font-heading text-3xl font-bold">Confidentialité</h1>
          <p className="mt-1 text-sm text-muted-foreground">Mise à jour le 7 octobre 2026.</p>
        </div>

        <Section title="En bref">
          <p>
            HoopPicks est un jeu de pronostics NBA en <strong className="text-foreground">points virtuels</strong> : aucun
            argent réel, aucun paiement. On ne garde que ce qu&apos;il faut pour faire tourner ton compte, on ne vend rien,
            on n&apos;affiche aucune publicité et on n&apos;utilise aucun traceur publicitaire.
          </p>
        </Section>

        <Section title="Ce que nous conservons">
          <ul className="list-inside list-disc space-y-1">
            <li>ton pseudo, ton adresse e-mail et ton mot de passe (uniquement sous forme hachée, illisible) ;</li>
            <li>ton avatar, ton équipe favorite et tes préférences de notification ;</li>
            <li>tes paris, ton solde de points, tes badges, tes ligues et tes duels ;</li>
            <li>
              si tu les actives, l&apos;adresse technique de ton appareil pour les notifications push (fournie par ton
              navigateur, elle ne contient ni ton nom ni ton numéro).
            </li>
          </ul>
          <p>
            Un seul cookie : celui de ta session, nécessaire pour rester connecté. Pas de cookie de mesure d&apos;audience
            ni de publicité.
          </p>
        </Section>

        <Section title="Ce que les autres joueurs voient">
          <p>
            Les joueurs connectés voient ton <strong className="text-foreground">profil public</strong> : pseudo, avatar,
            équipe favorite, points et rang, réussite, badges débloqués et tes tickets{" "}
            <strong className="text-foreground">déjà réglés</strong>. Tes tickets en attente restent privés jusqu&apos;à
            leur résultat. Ton adresse e-mail n&apos;est jamais affichée.
          </p>
        </Section>

        <Section title="Durée et suppression">
          <p>
            Tes données sont gardées tant que ton compte existe. Tu peux le supprimer à tout moment depuis{" "}
            <Link href="/settings" className="text-primary hover:underline">
              Paramètres
            </Link>{" "}
            : la suppression est immédiate et définitive, paris et ligues compris.
          </p>
        </Section>

        <Section title="Tes droits">
          <p>
            Conformément à la loi ivoirienne n° 2013-450 relative à la protection des données à caractère personnel et,
            pour les joueurs en Europe, au RGPD, tu peux accéder à tes données, les corriger, les supprimer ou t&apos;opposer
            à leur traitement. Pour toute demande :{" "}
            <a href={`mailto:${CONTACT}`} className="text-primary hover:underline">
              {CONTACT}
            </a>
            .
          </p>
        </Section>

        <Section title="NBA et marques">
          <p>
            HoopPicks est un projet indépendant, non affilié à la NBA, à ses équipes ni à ESPN. Les noms et logos des
            équipes appartiennent à leurs propriétaires et ne sont utilisés que pour identifier les matchs.
          </p>
        </Section>
      </main>
      <LegalFooter />
    </div>
  );
}
