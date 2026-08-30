import * as Sentry from "@sentry/nextjs";

// Même principe que instrumentation-client.ts : DSN vide = SDK désactivé.
Sentry.init({
  dsn: process.env.NEXT_PUBLIC_SENTRY_DSN,
  environment: process.env.NEXT_PUBLIC_SENTRY_ENVIRONMENT ?? "local",
  tracesSampleRate: 0,
});
