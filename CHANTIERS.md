# Chantiers à venir

Idées et travaux en attente. Rien ici n'est commencé tant que ce n'est pas demandé. Cocher ou retirer une ligne quand c'est fait.

## En attente (technique)

- [ ] **Traduction des actus** : passer à Azure Translator (choix du propriétaire, plus tard). Le quota à vie de DeepL (1 000 000 de caractères) est épuisé : les actus restent en anglais. Clé à mettre sur Railway.
- [ ] **Tester les notifications push** sur iPhone depuis l'app ajoutée à l'écran d'accueil (clés VAPID en place sur Railway).

## Design

- [ ] **Vitrine, connexion et inscription avec des photos** : première maquette pas convaincante pour le propriétaire (coucher de soleil orange, photos Pexels), à reprendre plus tard : https://claude.ai/artifact/GhH2mxn395Yttb2hasabbw

## Lancement public

- [ ] **Sauvegardes de la base** : activer les sauvegardes du service Postgres sur Railway (onglet Backups).
- [ ] **Suivi des erreurs** : créer un projet Sentry gratuit et renseigner `SENTRY_DSN` (Railway) et `NEXT_PUBLIC_SENTRY_DSN` (Vercel).
- [ ] **Clé admin** : vérifier que `ADMIN_API_KEY` est longue et aléatoire (au moins 32 caractères).
- [ ] **Quota d'e-mails Brevo** (300 par jour en gratuit) : surveiller les inscriptions au lancement ; au-delà, les e-mails de vérification ne partent plus (non bloquant).
- [ ] **Inscriptions en masse** : limiter le nombre d'inscriptions par adresse IP si des comptes robots apparaissent.
- [x] **Redéploiements** : sessions gardées en base depuis octobre 2026, une mise en ligne ne déconnecte plus personne.

## Coupe NBA (avant mi-novembre)

- [ ] **Pronostics de poules de la Coupe** : édition 2027 seulement (celle de 2026 a commencé le 30 octobre, trop tard). Vainqueur de chaque poule, repêchés, champion, à fermer avant le premier match.
- [ ] **Composition des poules 2027-28** à écrire dans `CupStandingsService.GROUPS` à l'annonce de la NBA (ni balldontlie ni ESPN ne la donnent ; celle de 2026-27 est en place).
- [x] **Tableau final** des quarts à la finale : page `/coupe`. **À faire le 27 novembre** : saisir les 8 équipes et l'heure du premier quart dans la console (onglet Saison).
- [x] **Trophée** au palmarès et icône de maillot pour le meilleur tableau.

## Petits jeux toute la saison

- [ ] **Le survivant** : chaque semaine, une équipe qui doit gagner ; éliminé à la première défaite ; une équipe une seule fois. Idéal dans une ligue.
- [ ] **Pick'em du soir** : le vainqueur de chaque match de la soirée, sans mise, un point par bon choix, classement à part.
- [ ] **Score exact** du match boosté du jour : le plus proche gagne.
- [ ] **Défis de la semaine** : missions (« gagne un combiné avec un outsider », « gagne un pari joueur ») qui rapportent des badges.

## Se retrouver et débattre

- [ ] **Le débat du jour** : une question par jour, deux camps, on vote puis on argumente dans son camp ; résultat partageable.
- [ ] **Quiz du jour** : 5 questions tirées de nos données, les mêmes pour tous, classement du jour et par ligue, score à partager. Puis défi quiz 1 contre 1.
- [ ] **Guerre des fans** : classement des équipes par la moyenne de leurs fans (équipe favorite), avec un fil par équipe.
- [ ] **Homme du match voté** par les joueurs après chaque match.
- [ ] **Paris tenus** : une prédiction publique (« Les Knicks en finale »), les autres sont d'accord ou non, verdict plus tard.

## Événements

- [ ] **Playoffs (avril)** : tableau à remplir, même mécanique que la Coupe.
- [ ] **All-Star (février)** : pronostics MVP, concours à 3 points, concours de dunks.
- [ ] **Tournoi de duels** dans une ligue : élimination directe sur plusieurs semaines.

## Idées en vrac

- [ ] **Badge « Dans la poche »** : app installée et notifications activées (petite récompense visible, sans points).
- [ ] **Handicaps alternatifs** : plusieurs lignes d'écart par match (ex. −5,5 / −2 / +3,5), chacune avec sa cote, au lieu d'une seule ligne où le favori prend le handicap négatif. À présenter repliées (« Plus d'écarts ») pour ne pas charger la page du match.

- [x] **Résumé vidéo des matchs terminés** : la vidéo officielle des highlights (chaîne YouTube de la NBA) intégrée avec le lecteur YouTube sur la page du match, onglet Résumé. Jamais de diffusion des matchs eux-mêmes (droits NBA / diffuseurs), au plus un lien vers NBA League Pass.

<!-- Ajouter ici les nouvelles idées au fil de l'eau. -->
