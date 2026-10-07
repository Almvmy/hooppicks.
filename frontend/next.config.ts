import type { NextConfig } from "next";
import { withSentryConfig } from "@sentry/nextjs";

const nextConfig: NextConfig = {
  experimental: {
    // Transitions de page natives (API View Transitions du navigateur via le
    // <ViewTransition> de React) : zéro dépendance d'animation, et un
    // navigateur qui ne la supporte pas affiche simplement la page sans animer.
    viewTransition: true,
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
