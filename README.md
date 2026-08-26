# HoopPicks

Pronostics NBA en points virtuels — aucun argent réel en jeu. Les joueurs parient des points sur les matchs de la saison NBA (moneyline, spread, total), grimpent dans un classement global et dans des ligues privées entre amis.

## Stack

| | |
|---|---|
| **Backend** | Java 21, Spring Boot 4.1.0, Spring Data JPA, PostgreSQL |
| **Frontend** | Next.js 16 (App Router, Turbopack), React 19, TypeScript, Tailwind CSS v4, TanStack Query |
| **Intégrations externes** | [balldontlie.io](https://balldontlie.io) (données NBA), API ESPN non-officielle (feuilles de match, effectifs, classement, stats joueurs), flux RSS ESPN (actualités), DeepL (traduction FR), Brevo (envoi d'e-mails) |

Vue d'ensemble de l'architecture et des décisions de conception : voir [CLAUDE.md](CLAUDE.md).
Référence des endpoints backend : voir [backend/API.md](backend/API.md).

## Prérequis

- Java 21+
- Node.js 20+
- PostgreSQL (local, une base vide suffit)

## Démarrage local

### 1. Base de données

Crée une base PostgreSQL locale (le nom `hooppicks` est indicatif, adapte selon `application.properties`) :

```bash
createdb hooppicks
```

Les tables sont créées/mises à jour automatiquement au démarrage (`spring.jpa.hibernate.ddl-auto=update`).

### 2. Backend

```bash
cd backend
cp src/main/resources/application.properties.exemple src/main/resources/application.properties
```

Renseigne dans ce fichier :
- `spring.datasource.url` / `username` / `password` — ta base Postgres locale
- `balldontlie.api-key` — clé gratuite sur [balldontlie.io](https://balldontlie.io)
- `admin.api-key` — un secret arbitraire pour toi seul (protège `/admin/**`, voir [CLAUDE.md](CLAUDE.md))
- `brevo.api-key` / `app.mail-from` — optionnel, seulement nécessaire pour tester le reset de mot de passe et la vérification d'email à l'inscription (API HTTP Brevo, pas SMTP — beaucoup d'hébergeurs bloquent les ports SMTP sortants)
- `deepl.api-key` — optionnel, seulement nécessaire pour la traduction des actualités (clé gratuite sur DeepL)

Puis lance :

```bash
./mvnw spring-boot:run
```

Le backend écoute sur `http://localhost:3001`.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Le frontend écoute sur `http://localhost:3000` et cible le backend via `BACKEND_API_URL` (défaut : `http://localhost:3001`).

### 4. Tests

```bash
cd backend && ./mvnw test
cd frontend && npx tsc --noEmit && npm run lint
```

## Notes

- **Pas d'argent réel** : le solde (`walletBalance`) est un compteur de points partant à 1000, pas un moyen de paiement.
- **Sessions en mémoire** : `SessionStore` ne persiste rien — redémarrer le backend déconnecte tout le monde (comportement volontaire pour un projet de cet ordre de grandeur, voir [CLAUDE.md](CLAUDE.md)).
- **Synchro NBA automatique** : toutes les 5 minutes (`NbaSyncScheduler`), qui déclenche aussi la résolution des paris en attente dès qu'un match passe à `FINISHED`, l'import des feuilles de match ESPN et un lot de stats joueurs ESPN. Effectifs et classement officiel ESPN sont resynchronisés une fois par jour (crons séparés, voir [CLAUDE.md](CLAUDE.md)).
