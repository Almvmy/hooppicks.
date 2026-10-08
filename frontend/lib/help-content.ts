// Contenu de la page d'aide. Chaque réponse décrit le comportement réel du
// code (BetController, BetResolutionService, NbaSyncScheduler…) : à mettre
// à jour en même temps que les règles, sinon l'aide ment.

export const SUPPORT_EMAIL = "almamyksg@gmail.com";

export interface FaqItem {
  id: string;
  question: string;
  answer: string;
  /** Lien vers l'endroit de l'app concerné, affiché sous la réponse. */
  link?: { href: string; label: string };
}

export interface FaqGroup {
  title: string;
  items: FaqItem[];
}

export const FAQ: FaqGroup[] = [
  {
    title: "Paris et points",
    items: [
      {
        id: "argent-reel",
        question: "Est-ce que je joue de l'argent réel ?",
        answer:
          "Non, jamais. Chaque semaine, tu reçois 1 000 points virtuels, sans aucune valeur monétaire : on ne peut ni en acheter, ni les retirer. HoopPicks n'est pas un site de paris d'argent.",
      },
      {
        id: "semaine-de-jeu",
        question: "Pourquoi mon solde revient à 1 000 points le lundi ?",
        answer:
          "Chaque lundi à 12h GMT, tout le monde repart avec 1 000 points, que tu aies tout misé, tout perdu ou beaucoup gagné. Ce solde sert seulement à parier : il ne compte pas au classement. Ainsi chaque semaine se joue à armes égales, et personne ne reste bloqué à zéro plus de quelques jours. Un pari posé avant lundi midi et réglé après compte pour la semaine où tu l'as posé : son gain va au classement de cette semaine-là, pas sur ton nouveau solde.",
      },
      {
        id: "plus-parier",
        question: "Pourquoi je ne peux plus parier sur ce match ?",
        answer:
          "Deux raisons possibles. Les paris ferment au coup d'envoi : une fois le match commencé, il n'est plus proposé. Et seuls les matchs de la semaine de jeu en cours sont ouverts : ceux d'après le lundi 12h GMT ouvrent ce lundi-là, avec la nouvelle semaine.",
        link: { href: "/matches", label: "Voir les matchs à venir" },
      },
      {
        id: "cote-change",
        question: "La cote peut-elle changer après ma validation ?",
        answer:
          "Non. La cote appliquée à ton ticket est celle du match au moment où tu valides, et elle ne bouge plus ensuite, quoi qu'il arrive.",
      },
      {
        id: "gain-combine",
        question: "Comment est calculé le gain d'un combiné ?",
        answer:
          "Les cotes de toutes tes sélections se multiplient, puis la mise est multipliée par cette cote totale. Exemple : 1,62 × 1,91 = 3,09 ; avec 50 points misés, le gain potentiel est de 155 points. Il faut que toutes les sélections soient gagnantes.",
      },
      {
        id: "paris-joueurs",
        question: "Comment marchent les paris joueurs ?",
        answer:
          "Sur la page d'un match, choisis une statistique (points, rebonds, passes, ou les trois cumulés) puis plus ou moins que la ligne. La ligne est la moyenne du joueur sur la saison, arrondie en ,5. Le pari est réglé d'après la feuille de match, en général moins d'une heure après la fin. Si le joueur ne joue pas, ta sélection est remboursée (cote de 1,00 dans un combiné).",
        link: { href: "/matches", label: "Choisir un match" },
      },
      {
        id: "ligne-joueur-change",
        question: "On me dit que la ligne d'un joueur a changé, pourquoi ?",
        answer:
          "Les moyennes des joueurs sont mises à jour plusieurs fois par jour. Si celle de ton joueur a bougé entre le moment où tu l'as ajouté et ta validation, on te prévient au lieu de poser le pari sur une autre ligne que celle que tu as vue : retire la sélection et ajoute-la de nouveau.",
      },
      {
        id: "meme-match",
        question: "Pourquoi je ne peux pas mettre deux paris sur le même match dans un ticket ?",
        answer:
          "Un ticket ne peut contenir qu'une sélection par match, pari joueur compris : deux issues du même match sont liées (un joueur qui marque beaucoup aide son équipe à gagner), et les combiner gonflerait la cote artificiellement. Fais deux tickets séparés si tu veux jouer deux marchés.",
      },
      {
        id: "quand-resolu",
        question: "Quand mon ticket est-il résolu ?",
        answer:
          "Automatiquement, quelques minutes après la fin du dernier match du ticket (les scores sont vérifiés toutes les 5 minutes). Les points sont crédités et tu reçois une notification. Un combiné reste en attente tant que tous ses matchs ne sont pas terminés.",
        link: { href: "/bets", label: "Voir mes paris" },
      },
      {
        id: "rembourse",
        question: "Mon ticket est « remboursé », qu'est-ce que ça veut dire ?",
        answer:
          "Le résultat est tombé pile sur la ligne de tes sélections (par exemple un total de 220 points sur une ligne à 220). Dans ce cas, personne ne gagne ni ne perd : ta mise t'est rendue.",
      },
      {
        id: "egalite-combine",
        question: "Une sélection de mon combiné est tombée pile sur la ligne, que se passe-t-il ?",
        answer:
          "Elle compte pour une cote de 1,00 : ton combiné reste gagnant si toutes les autres sélections le sont, et le gain est recalculé sans elle. Exemple : 100 points sur 1,50 × 2,00 × 1,91, la dernière à égalité → 100 × 1,50 × 2,00 = 300 points. « Mes paris » affiche alors la cote et le gain réellement appliqués.",
      },
      {
        id: "mise-debitee",
        question: "Ma mise a disparu de mon solde, c'est normal ?",
        answer:
          "Oui : la mise est débitée dès que tu valides le ticket. Si le ticket est gagnant, tu reçois le gain complet (mise comprise) à sa résolution.",
      },
    ],
  },
  {
    title: "Ligues et classement",
    items: [
      {
        id: "rejoindre-ligue",
        question: "Comment rejoindre la ligue d'un ami ?",
        answer:
          "Demande-lui le code d'invitation à 6 caractères affiché sur la page de sa ligue, puis va dans Ligues › Rejoindre et saisis-le.",
        link: { href: "/leagues", label: "Aller aux ligues" },
      },
      {
        id: "classement-calcul",
        question: "Comment est calculé le classement ?",
        answer:
          "Semaine par semaine. Tes points d'une semaine, c'est ton bilan sur les paris posés cette semaine-là : chaque ticket gagné ajoute son gain moins sa mise, chaque ticket perdu retire sa mise, un ticket remboursé ne compte pas. Ce total ne descend jamais sous 0 : une mauvaise semaine vaut 0, elle ne te fait pas perdre les points des autres. Le classement de la saison additionne tes semaines, celui du mois les semaines commencées dans le mois. Chaque ligue a aussi son propre classement, limité à ses membres.",
        link: { href: "/leaderboard", label: "Voir le classement" },
      },
      {
        id: "duels",
        question: "Comment défier un ami en duel ?",
        answer:
          "Depuis son profil (touche son pseudo dans un classement ou une ligue) ou depuis Compétition › Duels. S'il accepte, vous comparez vos points de classement de la semaine : le plus haut lundi 12h GMT gagne.",
        link: { href: "/duels", label: "Voir mes duels" },
      },
      {
        id: "pronostics-saison",
        question: "Où sont les pronostics de saison ?",
        answer:
          "Dans Compétition › Saison, et sur l'accueil tant qu'il t'en reste à faire. Quatre questions (champion, finalistes de chaque conférence, meilleur bilan), modifiables jusqu'à une semaine après le début de la saison régulière. Les choix des autres joueurs ne s'affichent qu'une fois les pronostics clos.",
        link: { href: "/saison", label: "Faire mes pronostics" },
      },
      {
        id: "badges",
        question: "Comment débloquer des badges ?",
        answer:
          "Ils se débloquent tout seuls en jouant : premier ticket, séries de victoires, combinés, gros gains… La liste complète et les conditions sont dans les règles ci-dessus et sur ta page Profil.",
        link: { href: "/profile", label: "Voir mes badges" },
      },
    ],
  },
  {
    title: "Compte et application",
    items: [
      {
        id: "email-verification",
        question: "Je n'ai pas reçu l'e-mail de confirmation.",
        answer:
          "Regarde dans tes spams, puis utilise « Renvoyer le lien » dans la bannière en haut de l'app. Ton compte reste entièrement utilisable en attendant.",
      },
      {
        id: "mot-de-passe-oublie",
        question: "J'ai oublié mon mot de passe.",
        answer:
          "Sur la page de connexion, clique sur « Mot de passe oublié ? » : tu recevras un lien valable 30 minutes pour en choisir un nouveau. Toutes tes autres sessions sont alors déconnectées, par sécurité.",
      },
      {
        id: "changer-pseudo",
        question: "Puis-je changer de pseudo ou d'e-mail ?",
        answer:
          "Oui, dans Paramètres, avec ton mot de passe. Un nouveau pseudo s'affiche immédiatement partout (classement, ligues). Seul un ancien lien de profil partagé ne fonctionnera plus.",
        link: { href: "/settings", label: "Ouvrir les paramètres" },
      },
      {
        id: "heures",
        question: "Les heures des matchs ne correspondent pas à mon heure locale.",
        answer:
          "Par défaut, l'app suit le fuseau horaire de ton appareil. Tu peux en choisir un autre dans Paramètres › Affichage et application, par exemple l'heure de l'Est US pour suivre les matchs à l'heure américaine.",
        link: { href: "/settings", label: "Changer le fuseau horaire" },
      },
      {
        id: "notifications-telephone",
        question: "Comment recevoir les notifications sur mon téléphone ?",
        answer:
          "Active « Sur cet appareil » dans Paramètres › Notifications. Sur iPhone, il faut d'abord ajouter HoopPicks à l'écran d'accueil depuis Safari, puis l'activer depuis l'app installée.",
        link: { href: "/settings", label: "Gérer les notifications" },
      },
      {
        id: "installer",
        question: "Peut-on installer HoopPicks comme une application ?",
        answer:
          "Oui, sans passer par un store : depuis Paramètres › Application (ou en bas de la page d'accueil), le bouton d'installation s'adapte à ton appareil. Sur iPhone, un guide pas à pas t'indique les gestes dans Safari.",
      },
      {
        id: "theme",
        question: "Il existe un mode clair ?",
        answer: "Oui : Paramètres › Affichage et application › Thème. Choisis Sombre, Clair, ou Système pour suivre ton appareil.",
      },
      {
        id: "supprimer-compte",
        question: "Comment supprimer mon compte ?",
        answer:
          "Dans Paramètres › Zone dangereuse, avec ton mot de passe. La suppression est immédiate et définitive : paris, ligues créées et historique sont effacés.",
      },
    ],
  },
];

/** Minuscules sans accents : « resolu » trouve « résolu », « equipe » trouve « Équipe ». */
export function normalizeForSearch(text: string): string {
  return text.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
}
