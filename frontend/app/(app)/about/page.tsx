import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function AboutPage() {
  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-bold">À propos de HoopPicks</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Ce que c&apos;est, ce que ça fait de tes données, et comment nous contacter.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">L&apos;essentiel</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2 text-sm text-muted-foreground">
          <p>
            HoopPicks est une application de pronostics sur les matchs de la saison NBA, en{" "}
            <strong className="text-foreground">points virtuels uniquement</strong>. Aucun
            argent réel n&apos;entre ou ne sort jamais du service — le solde affiché n&apos;a
            aucune valeur monétaire, ce n&apos;est ni un moyen de paiement, ni un jeu d&apos;argent.
          </p>
          <p>
            Les cotes utilisées sont fixées à la synchronisation des matchs et ne
            reflètent pas un marché de paris réel.
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Tes données</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2 text-sm text-muted-foreground">
          <p>Pour faire fonctionner ton compte, nous conservons :</p>
          <ul className="list-inside list-disc">
            <li>ton pseudo et ton adresse e-mail ;</li>
            <li>ton mot de passe, jamais en clair (uniquement sous forme hachée) ;</li>
            <li>ton historique de paris, tes ligues et tes préférences de notification.</li>
          </ul>
          <p>
            Ces informations ne sont ni vendues ni partagées avec des tiers. Tu peux
            supprimer ton compte et toutes les données associées à tout moment, depuis{" "}
            <a href="/settings" className="text-primary hover:underline">
              Paramètres
            </a>{" "}
            — l&apos;action est immédiate et irréversible.
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Contact</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          <p>
            Une question, un bug à signaler ? Écris-nous à{" "}
            <a href="mailto:almamyksg@gmail.com" className="text-primary hover:underline">
              almamyksg@gmail.com
            </a>
            .
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
