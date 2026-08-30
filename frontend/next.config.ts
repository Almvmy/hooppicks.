import type { NextConfig } from "next";
import { withSentryConfig } from "@sentry/nextjs";

const nextConfig: NextConfig = {
  /* config options here */
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
