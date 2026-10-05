"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { ArrowRight, BookOpen, ChevronDown, Layers, Lock, Mail, MessageCircleQuestion, Scale, Search, Timer, Trophy } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { badgeIcon } from "@/lib/badges";
import { FAQ, SUPPORT_EMAIL, normalizeForSearch } from "@/lib/help-content";
import { cn } from "@/lib/utils";

// Mêmes conditions que BadgeService (backend), dans le même ordre.
const BADGE_RULES = [
  { icon: "ticket", name: "Premier ticket", condition: "Place ton tout premier pari." },
  { icon: "repeat", name: "Habitué", condition: "10 paris placés au total." },
  { icon: "medal", name: "Vétéran", condition: "50 paris placés au total." },
  { icon: "flame", name: "Main chaude", condition: "3 paris gagnés d'affilée." },
  { icon: "cloud", name: "Sur un nuage", condition: "5 paris gagnés d'affilée." },
  { icon: "target", name: "Sniper", condition: "Au moins 10 paris résolus avec 60 % de réussite." },
  { icon: "crown", name: "Roi du multiple", condition: "Un ticket d'au moins 3 sélections." },
  { icon: "zap", name: "Gros coup", condition: "Un ticket rapportant au moins 500 points." },
  { icon: "coins", name: "Gros joueur", condition: "2 000 points misés au total." },
];

const SECTIONS = [
  { id: "regles", label: "Règles" },
  { id: "faq", label: "Questions fréquentes" },
  { id: "contact", label: "Contact" },
];

function RuleCard({ icon: Icon, title, children }: { icon: React.ElementType; title: string; children: React.ReactNode }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
            <Icon className="h-4 w-4" />
          </span>
          {title}
        </CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-2 text-sm text-muted-foreground">{children}</CardContent>
    </Card>
  );
}

export default function HelpPage() {
  const [query, setQuery] = useState("");
  const normalizedQuery = normalizeForSearch(query.trim());

  // Filtre sur la question ET la réponse : on cherche "remboursé", on doit
  // trouver la question qui en parle même si le mot n'est que dans la réponse.
  const filteredGroups = useMemo(() => {
    if (!normalizedQuery) return FAQ;
    const words = normalizedQuery.split(/\s+/);
    return FAQ.map((group) => ({
      ...group,
      items: group.items.filter((item) => {
        const haystack = normalizeForSearch(`${item.question} ${item.answer}`);
        return words.every((word) => haystack.includes(word));
      }),
    })).filter((group) => group.items.length > 0);
  }, [normalizedQuery]);

  const resultCount = filteredGroups.reduce((n, g) => n + g.items.length, 0);
  const isSearching = normalizedQuery.length > 0;

  const mailto = `mailto:${SUPPORT_EMAIL}?subject=${encodeURIComponent("HoopPicks : demande d'aide")}`;

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-8">
      <div>
        <h1 className="font-heading text-2xl font-bold">Aide et support</h1>
        <p className="mt-1 text-muted-foreground">
          Les règles du jeu, les réponses aux questions courantes, et comment nous joindre.
        </p>

        <div className="relative mt-5">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Rechercher : combiné, remboursé, ligue, notifications…"
            aria-label="Rechercher dans l'aide"
            className="pl-9"
          />
        </div>

        {!isSearching && (
          <nav aria-label="Sections de l'aide" className="mt-4 flex flex-wrap gap-2">
            {SECTIONS.map((section) => (
              <a
                key={section.id}
                href={`#${section.id}`}
                className="glass-inset-quiet rounded-full px-3 py-1.5 text-sm text-muted-foreground transition-colors hover:text-foreground"
              >
                {section.label}
              </a>
            ))}
          </nav>
        )}
      </div>

      {!isSearching && (
        <section id="regles" className="scroll-mt-24">
          <h2 className="mb-4 font-heading text-lg font-bold">Règles</h2>
          <div className="reveal-children grid gap-4 sm:grid-cols-2">
            <RuleCard icon={Lock} title="Points virtuels uniquement">
              <p>
                Tu démarres avec <strong className="text-foreground">1 000 points</strong>. Ils n&apos;ont aucune valeur
                monétaire : rien ne s&apos;achète, rien ne se retire.
              </p>
            </RuleCard>

            <RuleCard icon={BookOpen} title="Trois façons de parier">
              <ul className="flex flex-col gap-1.5">
                <li>
                  <strong className="text-foreground">Vainqueur</strong> (moneyline) : quelle équipe gagne le match.
                </li>
                <li>
                  <strong className="text-foreground">Écart</strong>{" "}(spread) : l&apos;équipe doit gagner avec au moins
                  l&apos;écart indiqué, ou perdre de moins que lui. « BOS -3.5 » : Boston doit gagner de 4 points ou plus.
                </li>
                <li>
                  <strong className="text-foreground">Total</strong>{" "}: le score cumulé des deux équipes sera-t-il au-dessus
                  ou en dessous de la ligne ?
                </li>
              </ul>
            </RuleCard>

            <RuleCard icon={Layers} title="Combinés">
              <p>
                Ajoute plusieurs matchs à un même ticket : les cotes se multiplient. Le ticket n&apos;est gagnant que si{" "}
                <strong className="text-foreground">toutes</strong> ses sélections le sont.
              </p>
              <p>Une seule sélection par match dans un ticket.</p>
            </RuleCard>

            <RuleCard icon={Timer} title="Jusqu'au coup d'envoi">
              <p>
                On peut parier sur un match tant qu&apos;il n&apos;a pas commencé. La cote affichée au moment où tu valides
                est celle appliquée : elle ne change plus ensuite.
              </p>
              <p>La mise est débitée dès la validation.</p>
            </RuleCard>

            <RuleCard icon={Scale} title="Résultat d'un ticket">
              <ul className="flex flex-col gap-1.5">
                <li>
                  <strong className="text-foreground">Gagné</strong> : tu reçois mise × cote totale.
                </li>
                <li>
                  <strong className="text-foreground">Perdu</strong> : au moins une sélection est perdante.
                </li>
                <li>
                  <strong className="text-foreground">Remboursé</strong>{" "}: le résultat tombe pile sur la ligne, ta mise
                  t&apos;est rendue.
                </li>
                <li>
                  Dans un combiné, une sélection à égalité compte pour une cote de 1,00 : le gain est recalculé sans elle.
                </li>
              </ul>
              <p>
                C&apos;est automatique, quelques minutes après la fin du dernier match du ticket, avec une notification à la
                clé.
              </p>
            </RuleCard>

            <RuleCard icon={Trophy} title="Classement et ligues">
              <p>
                Le classement général compare tous les joueurs. Crée une ligue privée et partage son code à 6
                caractères pour avoir aussi votre classement entre amis.
              </p>
            </RuleCard>
          </div>

          <Card className="mt-4">
            <CardHeader>
              <CardTitle className="text-base">Badges</CardTitle>
            </CardHeader>
            <CardContent>
              <ul className="grid gap-2 sm:grid-cols-2">
                {BADGE_RULES.map((badge) => {
                  const Icon = badgeIcon(badge.icon);
                  return (
                    <li key={badge.name} className="glass-inset-quiet flex items-start gap-3 rounded-xl px-3 py-2.5">
                      <Icon className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
                      <div>
                        <p className="text-sm font-medium">{badge.name}</p>
                        <p className="text-xs text-muted-foreground">{badge.condition}</p>
                      </div>
                    </li>
                  );
                })}
              </ul>
            </CardContent>
          </Card>
        </section>
      )}

      <section id="faq" className="scroll-mt-24">
        <h2 className="mb-1 font-heading text-lg font-bold">Questions fréquentes</h2>
        {isSearching && (
          <p className="mb-3 text-sm text-muted-foreground" aria-live="polite">
            {resultCount === 0
              ? "Aucune réponse ne correspond."
              : `${resultCount} réponse${resultCount > 1 ? "s" : ""} pour « ${query.trim()} »`}
          </p>
        )}

        <div className="mt-3 flex flex-col gap-6">
          {filteredGroups.map((group) => (
            <div key={group.title}>
              <h3 className="mb-2 text-xs font-bold uppercase tracking-wide text-muted-foreground">{group.title}</h3>
              <div className="flex flex-col gap-2">
                {group.items.map((item) => (
                  // <details> natif : clavier, lecteur d'écran et ouverture
                  // depuis un lien #ancre fonctionnent sans JS. En recherche,
                  // les réponses s'affichent déjà ouvertes.
                  <details
                    key={`${item.id}-${isSearching}`}
                    id={item.id}
                    open={isSearching}
                    className="glass group scroll-mt-24 rounded-2xl"
                  >
                    <summary className="flex cursor-pointer list-none items-center justify-between gap-3 px-4 py-3 text-sm font-medium [&::-webkit-details-marker]:hidden">
                      {item.question}
                      <ChevronDown className="h-4 w-4 shrink-0 text-muted-foreground transition-transform group-open:rotate-180" />
                    </summary>
                    <div className="px-4 pb-4 text-sm text-muted-foreground">
                      <p>{item.answer}</p>
                      {item.link && (
                        <Link
                          href={item.link.href}
                          className="mt-2 inline-flex items-center gap-1 text-primary hover:underline"
                        >
                          {item.link.label}
                          <ArrowRight className="h-3.5 w-3.5" />
                        </Link>
                      )}
                    </div>
                  </details>
                ))}
              </div>
            </div>
          ))}
        </div>
      </section>

      <section id="contact" className="scroll-mt-24">
        <Card className={cn(isSearching && resultCount === 0 && "glass-accent")}>
          <CardContent className="flex flex-col gap-3 pt-6 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-start gap-3">
              <MessageCircleQuestion className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
              <div>
                <p className="font-medium">
                  {isSearching && resultCount === 0 ? "Pas trouvé ta réponse ?" : "Une question, un bug ?"}
                </p>
                <p className="text-sm text-muted-foreground">
                  Écris-nous en précisant ton pseudo et ce que tu faisais : on répond par e-mail.
                </p>
              </div>
            </div>
            <a
              href={mailto}
              className="glass-inset inline-flex shrink-0 items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium"
            >
              <Mail className="h-4 w-4" />
              {SUPPORT_EMAIL}
            </a>
          </CardContent>
        </Card>
        <p className="mt-3 text-center text-xs text-muted-foreground">
          Données personnelles et mentions :{" "}
          <Link href="/about" className="text-primary hover:underline">
            À propos
          </Link>
        </p>
      </section>
    </div>
  );
}
