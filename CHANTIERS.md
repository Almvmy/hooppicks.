# Chantiers à venir

Idées et travaux en attente. Rien ici n'est commencé tant que ce n'est pas demandé. Cocher ou retirer une ligne quand c'est fait.

## En attente (technique)

- [ ] **Tester les notifications push** sur iPhone depuis l'app ajoutée à l'écran d'accueil (clés VAPID en place sur Railway).

## Lancement public

- [ ] **Sauvegardes de la base** : activer les sauvegardes du service Postgres sur Railway (onglet Backups).
- [ ] **Suivi des erreurs** : créer un projet Sentry gratuit et renseigner `SENTRY_DSN` (Railway) et `NEXT_PUBLIC_SENTRY_DSN` (Vercel).
- [ ] **Clé admin** : vérifier que `ADMIN_API_KEY` est longue et aléatoire (au moins 32 caractères).
- [ ] **Quota d'e-mails Brevo** (300 par jour en gratuit) : surveiller les inscriptions au lancement ; au-delà, les e-mails de vérification ne partent plus (non bloquant).
- [ ] **Inscriptions en masse** : limiter le nombre d'inscriptions par adresse IP si des comptes robots apparaissent.
- [x] **Redéploiements** : sessions gardées en base depuis octobre 2026, une mise en ligne ne déconnecte plus personne.

## Coupe NBA (avant mi-novembre)

- [ ] **Pronostics de la Coupe** : avant le premier match, vainqueur de chaque poule, 2 repêchés, champion. Classement à part, sur le modèle des pronostics de saison (questions, verrou, réponses saisies par un admin).
- [ ] **Composition des poules** saisie une fois dans la console admin (ni balldontlie ni ESPN ne la donnent de façon fiable). Vérifier le format 2026 (jusqu'ici 6 poules de 5, 8 qualifiés).
- [ ] **Tableau final** des quarts à la finale (début décembre), plus de points pour les tours avancés. Réutilisable pour les playoffs.
- [ ] **Trophée « Coupe NBA »** au palmarès + badge pour le meilleur pronostiqueur.

## Petits jeux toute la saison

- [ ] **Le survivant** : chaque semaine, une équipe qui doit gagner ; éliminé à la première défaite ; une équipe une seule fois. Idéal dans une ligue.
- [ ] **Pick'em du soir** : le vainqueur de chaque match de la soirée, sans mise, un point par bon choix, classement à part.
- [ ] **Score exact** du match boosté du jour : le plus proche gagne.
- [ ] **Défis de la semaine** : missions (« gagne un combiné avec un outsider », « gagne un pari joueur ») qui rapportent des badges.

## Événements

- [ ] **Playoffs (avril)** : tableau à remplir, même mécanique que la Coupe.
- [ ] **All-Star (février)** : pronostics MVP, concours à 3 points, concours de dunks.
- [ ] **Tournoi de duels** dans une ligue : élimination directe sur plusieurs semaines.

## Idées en vrac

- [ ] **Badge « Dans la poche »** : app installée et notifications activées (petite récompense visible, sans points).
- [ ] **Handicaps alternatifs** : plusieurs lignes d'écart par match (ex. −5,5 / −2 / +3,5), chacune avec sa cote, au lieu d'une seule ligne où le favori prend le handicap négatif. À présenter repliées (« Plus d'écarts ») pour ne pas charger la page du match.

- [ ] **Résumé vidéo des matchs terminés** : la vidéo officielle des highlights (chaîne YouTube de la NBA) intégrée avec le lecteur YouTube sur la page du match, onglet Résumé. Jamais de diffusion des matchs eux-mêmes (droits NBA / diffuseurs), au plus un lien vers NBA League Pass.

<!-- Ajouter ici les nouvelles idées au fil de l'eau. -->
