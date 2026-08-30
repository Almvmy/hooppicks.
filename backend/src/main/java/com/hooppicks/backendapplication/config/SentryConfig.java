package com.hooppicks.backendapplication.config;

import io.sentry.Sentry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Initialisation manuelle du SDK Sentry (pas sentry-spring-boot-starter, cf.
 * pom.xml). Si sentry.dsn est vide (dev local par défaut), Sentry.init()
 * avec un DSN vide désactive le SDK proprement : mêmes appels
 * Sentry.captureException() ailleurs dans le code, juste no-op tant qu'aucun
 * DSN n'est configuré. Même principe que brevo.api-key/deepl.api-key :
 * fonctionnalité optionnelle, dégradée gracieusement plutôt que de bloquer
 * le démarrage.
 */
@Configuration
public class SentryConfig {

    @Value("${sentry.dsn:}")
    private String dsn;

    @Value("${sentry.environment:local}")
    private String environment;

    @PostConstruct
    public void init() {
        Sentry.init(options -> {
            options.setDsn(dsn);
            options.setEnvironment(environment);
            // Erreurs uniquement : pas de traçage de perf, ça consommerait le
            // quota gratuit pour un signal qu'on n'a pas demandé.
            options.setTracesSampleRate(0.0);
        });
    }
}
