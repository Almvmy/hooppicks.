package com.hooppicks.backendapplication.live;

import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Notifications des matchs suivis, chaque minute : la synchro (5 min) est
 * trop lente pour annoncer une fin de quart-temps à temps. Ne coûte presque
 * rien sans match suivi en cours, et réutilise le cache du direct.
 */
@Component
@ConditionalOnProperty(prefix = "nba.sync", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class MatchFollowScheduler {

    private static final Logger log = LoggerFactory.getLogger(MatchFollowScheduler.class);

    private final MatchFollowService followService;

    public MatchFollowScheduler(MatchFollowService followService) {
        this.followService = followService;
    }

    @Scheduled(fixedRate = 60 * 1000, initialDelay = 45 * 1000)
    public void run() {
        try {
            int sent = followService.notifyFollowers(Instant.now());
            if (sent > 0) log.info("{} notification(s) de match suivi envoyée(s)", sent);
        } catch (Exception e) {
            log.warn("Notifications des matchs suivis échouées", e);
            Sentry.captureException(e);
        }
    }
}
