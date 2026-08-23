# Référence API — HoopPicks backend

Base URL locale : `http://localhost:3001`. Toutes les réponses sont en JSON.

**Auth par cookie de session** (`hp_session`, httpOnly) sauf mention contraire — posé par `/auth/login` ou `/auth/register`. Les endpoints marqués 🔒 renvoient `401` sans cookie valide.

## Auth (`/auth`)

| Méthode | Route | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | — | Crée un compte (`username`, `email`, `password`), pose le cookie de session. `409` si l'email existe déjà. |
| POST | `/auth/login` | — | `email` + `password`. `401` si invalide, `429` si trop de tentatives récentes (`LoginAttemptService`). |
| GET | `/auth/me` | 🔒 | Profil de l'utilisateur connecté. |
| PATCH | `/auth/profile` | 🔒 | Met à jour équipe favorite / avatar (numéro, poste, palette, icône) — champs optionnels, validés côté serveur contre une liste fermée. |
| PATCH | `/auth/notification-preferences` | 🔒 | Active/désactive les 3 types de notifications. |
| POST | `/auth/change-password` | 🔒 | Nécessite le mot de passe actuel. Invalide toutes les sessions actives après succès. |
| POST | `/auth/change-email` | 🔒 | Nécessite le mot de passe actuel. `409` si le nouvel email est déjà pris. |
| POST | `/auth/delete-account` | 🔒 | Nécessite le mot de passe actuel. Suppression définitive (`AccountDeletionService`). |
| POST | `/auth/forgot-password` | — | `email`. Toujours `200`, que le compte existe ou non (anti-énumération). Rate-limité à 3/heure/email. |
| POST | `/auth/reset-password` | — | `token` + `newPassword`. `400` si token invalide/expiré/déjà utilisé. |
| POST | `/auth/logout` | — | Invalide la session courante et expire le cookie. |

## Paris (`/bets`)

| Méthode | Route | Auth | Description |
|---|---|---|---|
| GET | `/bets` | 🔒 | Historique des tickets de l'utilisateur, plus récents d'abord. |
| POST | `/bets` | 🔒 | Place un ticket (`selections[]` + `stake`). Voir contraintes métier dans [CLAUDE.md](../CLAUDE.md#décisions-darchitecture-à-connaître-avant-de-toucher-au-code) (atomicité, un match max par ticket, cotes serveur autoritaires). `400` si solde insuffisant, match fermé/introuvable, ou doublon de match dans le ticket. |

## Portefeuille (`/wallet`)

| Méthode | Route | Auth | Description |
|---|---|---|---|
| GET | `/wallet` | 🔒 | Solde actuel en points. |
| GET | `/wallet/transactions` | 🔒 | Historique des transactions (mises, gains, remboursements). |

## Ligues (`/leagues`)

| Méthode | Route | Auth | Description |
|---|---|---|---|
| GET | `/leagues` | 🔒 | Ligues dont l'utilisateur est membre. |
| POST | `/leagues` | 🔒 | Crée une ligue (`name`). L'utilisateur en devient propriétaire et premier membre. |
| GET | `/leagues/preview/{code}` | 🔒 | Aperçu d'une ligue via son code d'invitation, avant de la rejoindre. `404` si code invalide. |
| POST | `/leagues/join` | 🔒 | Rejoint une ligue via `inviteCode`. `400` si code invalide. |
| GET | `/leagues/{id}/members` | 🔒 | Membres de la ligue. `403` si l'utilisateur n'en fait pas partie. |
| GET | `/leagues/{id}/activity` | 🔒 | Activité récente (arrivées, départs...). `403` si non-membre. |
| GET | `/leagues/{id}/leaderboard` | 🔒 | Classement propre à la ligue. `403` si non-membre. |
| POST | `/leagues/{id}/leave` | 🔒 | Quitte la ligue. |

## Matchs, équipes, joueurs

| Méthode | Route | Auth | Description |
|---|---|---|---|
| GET | `/matches` | — (public) | Calendrier complet, toutes saisons synchronisées. |
| GET | `/matches/{id}` | — (public) | Détail d'un match. |
| GET | `/teams` | — (public) | Classement des équipes NBA (Elo). |
| GET | `/players?search=...&teamId=...` | — (public) | Recherche de joueurs (3 caractères min), locale puis fallback balldontlie.io. |

## Classement, badges, notifications

| Méthode | Route | Auth | Description |
|---|---|---|---|
| GET | `/leaderboard` | — (public) | Classement global toutes ligues confondues. |
| GET | `/badges` | 🔒 | Badges débloqués/à débloquer par l'utilisateur. |
| GET | `/notifications` | 🔒 | Notifications de l'utilisateur. |
| PATCH | `/notifications/{id}/read` | 🔒 | Marque une notification comme lue. |

## Actualités (`/news`)

| Méthode | Route | Auth | Description |
|---|---|---|---|
| GET | `/news` | — (public) | Dernières actus NBA (ESPN, traduites en FR), cache 15 min côté serveur. |

## Admin — clé statique (`/admin/**`)

🔑 Nécessite l'en-tête `X-Admin-Key: <admin.api-key>`. Réservé à un usage manuel (curl/Postman) ou un futur job externe — jamais appelé depuis le frontend. Refusé (`403`) si la clé est absente/invalide ou si `admin.api-key` n'est pas configurée côté serveur. Chaque accès est loggé (méthode, route, IP).

| Méthode | Route | Description |
|---|---|---|
| POST | `/admin/nba/sync-teams` | Resynchronise les équipes depuis balldontlie.io. |
| GET | `/admin/nba/players-count` | Nombre de joueurs en base. |
| POST | `/admin/nba/sync-games?daysAhead=&startDate=` | Resynchronise les matchs sur une fenêtre de dates. |
| POST | `/admin/bets/resolve` | Force la résolution des paris en attente. |
| POST | `/admin/users/promote?email=` | Passe un utilisateur admin (`isAdmin=true`). |

## Admin — session (`/console/**`)

🔒 Nécessite un cookie de session valide **et** `User.isAdmin = true` (`401` si non connecté, `403` si connecté mais non-admin). Utilisé par la page `/admin` du frontend.

| Méthode | Route | Description |
|---|---|---|
| GET | `/console/status` | Vue d'ensemble : dernière synchro, nb utilisateurs/matchs, paris en attente. |
| POST | `/console/sync-teams` | Équivalent de `/admin/nba/sync-teams`, avec attribution de l'admin qui déclenche. |
| POST | `/console/sync-games?daysAhead=&startDate=` | Équivalent de `/admin/nba/sync-games`. |
| POST | `/console/resolve-bets` | Équivalent de `/admin/bets/resolve`. |

## Divers

| Méthode | Route | Auth | Description |
|---|---|---|---|
| GET | `/health` | — | Ping simple pour vérifier que le backend répond. |
