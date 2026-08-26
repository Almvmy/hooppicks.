# HoopPicks — contexte projet

Pronostics NBA en points virtuels (pas d'argent réel). Monorepo `backend/` (Spring Boot) + `frontend/` (Next.js). Voir [README.md](README.md) pour le setup.

## Décisions d'architecture à connaître avant de toucher au code

**Cotes fixes, pas de marché dynamique.** Les cotes (`moneylineHome`, `spreadOddsHome`, `totalOddsOver`, etc.) sont stockées à plat directement sur `Match`, fixées à la synchro et ne bougent plus. Décision assumée (commentaire dans `Match.java`), pas un raccourci à corriger.

**Double mécanisme admin, volontairement séparé** :
- `/admin/**` (`NbaSyncController`, `BetAdminController`, `AdminUserController`) — protégé par une clé statique dans l'en-tête `X-Admin-Key` (`AdminAuthFilter`). Pensé pour toi en curl/Postman ou un futur job externe, jamais exposé au frontend. Chaque accès (autorisé ou refusé) est loggé avec méthode/route/IP.
- `/console/**` (`AdminConsoleController`) — protégé par cookie de session + flag `User.isAdmin`, utilisé par la page `/admin` du frontend. Couvre aussi la gestion des utilisateurs (recherche, promotion/rétrogradation, suppression), la correction manuelle de matchs (score/statut, pour rattraper une synchro externe fausse) et la liste des paris en attente.
- Les deux couvrent en partie les mêmes actions (sync, résolution des paris) avec des niveaux d'audit différents : `/admin/**` logue qui/quand/depuis où (IP) faute d'identité utilisateur ; `/console/**` connaît l'utilisateur admin exact. Ne pas fusionner sans réfléchir à cet écart.
- **Pas de rôle "super-admin".** N'importe quel admin peut rétrograder n'importe quel autre admin via `/console/**` (sauf lui-même) — décision assumée : le pire cas est réversible en te repromouvant via `/admin/users/promote` avec la clé statique, que tu es seul à connaître. Ajouter une hiérarchie d'admin serait de la complexité inutile pour ce risque-là à cette échelle.

**Sessions en mémoire (`SessionStore`)**, pas en base ni Redis. Un redémarrage backend déconnecte tout le monde — accepté pour ce projet, pas un bug.

**`placeBet` (`BetController`) est le chemin financier critique** :
- `@Transactional` + verrou pessimiste (`UserRepository.findByIdForUpdate`, `SELECT ... FOR UPDATE`) pour empêcher un double-débit sur double-clic/double-onglet.
- Un seul match par ticket (`Set<matchId>` dédupliqué côté serveur) — deux issues contradictoires sur le même match n'ont pas de sens.
- Les cotes appliquées viennent toujours des matchs rechargés côté serveur, jamais de `request.selections().odds()` envoyé par le client (protection contre une cote périmée renvoyée volontairement).
- Couvert par [`BetControllerTest`](backend/src/test/java/com/hooppicks/backendapplication/controller/BetControllerTest.java).

**Résolution des paris automatique.** `NbaSyncScheduler` tourne toutes les 5 min, appelle `NbaSyncService.syncGames()`, qui déclenche `BetResolutionService.resolvePendingBets()` à la fin. Pas besoin d'un job séparé pour ça — c'est déjà branché.

**Reset de mot de passe** (`PasswordResetService`) : rate-limité (3 demandes/heure/email), ne révèle jamais si l'email existe (même réponse 200 dans les deux cas, y compris si l'envoi échoue), invalide toutes les sessions actives de l'utilisateur une fois le mot de passe changé. Couvert par [`PasswordResetServiceTest`](backend/src/test/java/com/hooppicks/backendapplication/security/PasswordResetServiceTest.java).

**Envoi d'email via l'API HTTP de Brevo (`EmailService`), pas SMTP.** Railway (et pas mal d'hébergeurs PaaS) bloque les ports SMTP sortants (587 et 465 testés, timeout systématique en prod) — l'API HTTP de Brevo passe par le port 443 comme n'importe quel autre appel externe, jamais bloquée pareil. Configuré via `brevo.api-key` (local) / `BREVO_API_KEY` (prod), voir `application-prod.properties`.

**Actualités (`NewsService`)** : cache 15 min, ESPN via RSS traduit en français via DeepL (un seul appel API pour titre+description de tous les articles, pas un appel par champ). Si ESPN est indisponible, garde le dernier cache connu plutôt que de vider la page. Un seul rafraîchissement à la fois (`AtomicBoolean`, pas de `synchronized` bloquant) — les autres appelants reçoivent le cache existant immédiatement.

**Intégration ESPN (`espn/`)** — deuxième source de données NBA, en complément de balldontlie (free tier limité : pas de feuille de match détaillée, pas de distinction actif/retraité côté joueurs, pas de classement officiel). API non documentée d'ESPN, sans clé.
- `EspnStatsClient.fetchWithRetry()` envoie un `User-Agent: curl/8.7.1` — pas un hasard. Akamai (le CDN d'ESPN) bloque en 403 tout ce qui ressemble à un navigateur (Chrome/Firefox), un User-Agent vide, ou même le UA par défaut de Java, mais laisse passer un UA `curl/x.y.z`. Vérifié en direct avec 3 clients HTTP indépendants (curl, .NET, `java.net.http.HttpClient`) avant de fixer ce choix — ne pas revenir à un UA "réaliste" de navigateur en pensant mieux faire, c'est exactement ce qui se fait bloquer.
- Ce projet est sur Spring Boot 4.1 → Jackson 3.x, sous le nouveau groupId `tools.jackson.*`, pas `com.fasterxml.jackson.*` (qui reste utilisé uniquement pour les annotations, `jackson-annotations` n'a pas été renommé). `EspnStatsClient` doit importer `tools.jackson.databind.JsonNode`, jamais `com.fasterxml.jackson.databind.JsonNode` — ce dernier compile très bien mais échoue à l'exécution (le convertisseur JSON de `RestTemplate` ne sait pas produire ce type-là), une erreur qu'aucun test mocké ne détecte puisqu'il court-circuite la désérialisation réelle.
- Feuille de match (`EspnStatsService`, `PlayerMatchStat`) : lien match↔event ESPN résolu par date + sigles d'équipe à la synchro (les deux API n'ont aucun ID en commun), traité par lots de 3 sur le tick de `NbaSyncScheduler` — jamais dans la grosse transaction de `NbaSyncService.doSyncGames`, c'est exactement ce genre de traitement en un seul paquet qui avait fait dépasser la mémoire allouée sur Railway par le passé.
- Effectifs (`EspnRosterService`, `RosterPlayer`, avec photo et statut blessure) : upsert par id athlète ESPN (jamais delete-and-recreate) pour que les moyennes saison déjà synchronisées survivent au rafraîchissement quotidien (cron 6h). Corrige un vrai bug : la recherche joueur cherchait avant dans balldontlie, qui ne distingue pas actif/retraité en free tier.
- Classement officiel (`EspnStandingsService`, champs `wins`/`losses`/`streak`/`conferenceSeed`/`gamesBehind`/`logoUrl` sur `Team`) : distinct de l'Elo interne, qui reste seul utilisé pour le calcul des cotes (`OddsService`) — ne pas les confondre ni les fusionner. Cron 6h05, un seul appel pour les 30 équipes.
- Stats saison par joueur (`EspnPlayerStatsService`) : ~550 joueurs, un appel ESPN chacun → traité par lots de 8 sur le tick du scheduler principal, priorité "jamais synchronisé d'abord puis le plus ancien" (`RosterPlayerRepository.findAllOrderByStatsUpdatedAtAscNullsFirst`), un passage complet prend plusieurs heures puis boucle en continu.
- Recherche joueurs (`PlayerController`) : cherche dans `RosterPlayer` (effectifs ESPN actuels) ; ne retombe sur balldontlie (`NbaSyncService.searchAndCachePlayers`, filet de secours) que si `RosterPlayer` est **totalement vide** (ESPN jamais synchronisé avec succès), jamais sur un simple "cette recherche n'a rien trouvé" — sinon chercher un joueur retraité redonnerait le bug qu'on corrige.
- Couvert par [`EspnStatsClientTest`](backend/src/test/java/com/hooppicks/backendapplication/espn/EspnStatsClientTest.java) (mocké, ne teste pas le réseau réel). `nba.sync.scheduler-enabled=false` désactive `NbaSyncScheduler` (donc tout l'ESPN qui y est accroché) pendant les tests, pour ne jamais taper ESPN pour de vrai en CI.

**Vérification d'email (`EmailVerificationService`) — non bloquante.** Un compte reste pleinement utilisable sans email vérifié (pas d'argent réel en jeu) ; une bannière rappelle juste de vérifier tant que ce n'est pas fait. Même schéma que le reset de mot de passe (token à usage unique 24h, rate-limité). Limite connue et acceptée : un email qui n'existe pas vraiment est accepté par Brevo à l'envoi (rejet asynchrone via bounce, jamais observé côté backend) — le compte reste alors indéfiniment non-vérifié, ce qui est le comportement honnête plutôt qu'un blocage à l'inscription. Redéclenchée automatiquement si l'email change (`change-email`). Couvert par [`EmailVerificationServiceTest`](backend/src/test/java/com/hooppicks/backendapplication/security/EmailVerificationServiceTest.java).

**Profils publics (`PublicProfileController`, `GET /users/{username}`)** : accessible à tout utilisateur connecté (pas anonyme, contrairement à `/leaderboard` ou `/matches`). Ne renvoie qu'un sous-ensemble sûr de `User` (`PublicProfileDto`) — jamais l'email. N'affiche que les **badges débloqués** (contrairement à `/badges`, qui renvoie aussi les verrouillés pour son propre profil) : les objectifs personnels n'ont pas d'intérêt à être exposés sur le profil de quelqu'un d'autre.

**Changer de pseudo (`change-username`) est volontairement permis**, au même titre que l'email — le pseudo n'est qu'un identifiant d'affichage, pas un secret. Le classement/les ligues/l'activité relisent le pseudo en direct depuis `User` (jamais une copie figée), donc un renommage se répercute immédiatement partout dans l'app. Seul effet de bord : un lien externe déjà partagé vers `/u/ancien-pseudo` renvoie `404` après renommage — accepté, ne concerne que des liens sortis de l'app.

## Conventions

- **Logging** : SLF4J (`LoggerFactory.getLogger`), jamais `System.out.println` ni `printStackTrace()`.
- **Dates/heures affichées côté frontend** : helpers centralisés dans [`lib/utils.ts`](frontend/lib/utils.ts) (`formatMatchDate`, `formatMatchTime`, `formatRelativeTime`, `getDayLabel`) — ne pas dupliquer `toLocaleDateString(...)` dans les composants.
- **Requêtes React Query** : toujours gérer `isError` en plus de `isLoading` sur les pages authentifiées — un échec réseau silencieux qui rend un état "vide" est considéré comme un bug ici, pas un détail.
- **Commentaires** : le code existant commente le "pourquoi", pas le "quoi" — garder ce style plutôt que d'ajouter des commentaires descriptifs.

## Historique récent notable

Un audit combiné backend (sécurité/logique métier/tests) + frontend (UX/accessibilité/hygiène du code) a été mené et corrigé en août 2026 : atomicité de `placeBet`, double mécanisme admin audité, tests ajoutés sur le code sensible (paris, reset mot de passe), remplacement des `println` par un vrai logger, gestion d'erreur réseau sur les pages authentifiées, accessibilité de l'éditeur d'avatar.

Suite à cet audit, plusieurs fonctionnalités ont été ajoutées : vérification d'email non bloquante, changement de pseudo, profils publics consultables (`/u/[username]`) avec pseudos cliquables dans le classement/les ligues, page "À propos", console admin enrichie (gestion utilisateurs, correction manuelle de match, visibilité des paris en attente), badges avec icône par badge + tri "débloqués d'abord". Le logo (`LogoSymbol.tsx`, `icon.svg`, `apple-icon.tsx`) a aussi été redessiné (monogramme "HP" sur un ballon, remplace l'ancien check).

Fin août 2026, intégration ESPN complète (voir section dédiée plus haut) : feuille de match par joueur, effectifs actuels avec photo et statut blessure, classement officiel par conférence distinct de l'Elo, moyennes saison par joueur, logos d'équipe, fiche joueur cliquable (photo + stats complètes) partout où un joueur apparaît. Recherche joueurs corrigée au passage (cherchait avant dans balldontlie, sans distinction actif/retraité). Voir l'historique git pour le détail des commits.
