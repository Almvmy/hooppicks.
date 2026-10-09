import type { NextConfig } from "next";
import { withSentryConfig } from "@sentry/nextjs";

const nextConfig: NextConfig = {
  // Tester sur un vrai téléphone en Wi-Fi (http://<IP du PC>:3000) : sans ça,
  // le serveur de dev bloque ses fichiers internes pour toute autre adresse que
  // localhost, la page s'affiche mais rien ne marche (connexion impossible).
  // Adresses de réseau local seulement, et sans effet en production.
  allowedDevOrigins: ["10.*.*.*", "192.168.*.*", "172.*.*.*"],
  experimental: {
    // Plus de drapeau viewTransition : depuis Next 16.4, les transitions de
    // page (<ViewTransition> de React) marchent sans configuration.
    // Cache disque de Turbopack en dev (actif par défaut depuis 16.1) : il
    // grossissait d'environ 1 Go par heure dans .next/dev et a rempli le
    // disque de la machine de dev plusieurs fois. Démarrage un peu plus lent.
    turbopackFileSystemCacheForDev: false,
  },
};

// org/project/authToken absents = pas de credentials Sentry configurés :
// le plugin de build saute l'upload des source maps sans faire échouer le
// build (silent: true pour ne pas polluer les logs avec cet avertissement
// tant que le projet Sentry n'est pas créé). Le SDK runtime, lui, fonctionne
// indépendamment de ça dès que NEXT_PUBLIC_SENTRY_DSN est renseigné.
export default withSentryConfig(nextConfig, {
  org: process.env.SENTRY_ORG,
  project: process.env.SENTRY_PROJECT,
  authToken: process.env.SENTRY_AUTH_TOKEN,
  silent: true,
});
