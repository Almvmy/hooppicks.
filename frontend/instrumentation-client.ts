import * as Sentry from "@sentry/nextjs";

// NEXT_PUBLIC_SENTRY_DSN vide/absent : Sentry.init() no-op proprement, même
// principe que sentry.dsn côté backend (cf. SentryConfig.java). Pas de
// tracesSampleRate ni de Session Replay : erreurs seulement, pour rester
// dans le quota gratuit d'un projet sans trafic réel pour l'instant.
Sentry.init({
  dsn: process.env.NEXT_PUBLIC_SENTRY_DSN,
  environment: process.env.NEXT_PUBLIC_SENTRY_ENVIRONMENT ?? "local",
  tracesSampleRate: 0,
});

export const onRouterTransitionStart = Sentry.captureRouterTransitionStart;
