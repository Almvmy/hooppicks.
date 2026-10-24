package com.hooppicks.backendapplication.favorite;

import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

// Même interrupteur que les autres tâches de fond (NbaSyncScheduler).
// Classe à part du service : ses méthodes @Transactional ne s'appliquent
// que appelées de l'extérieur (proxy Spring, cf. EspnStatsService).
@Component
@ConditionalOnProperty(prefix = "nba.sync", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class FavoriteTeamNotifierScheduler {

    private static final Logger log = LoggerFactory.getLogger(FavoriteTeamNotifierScheduler.class);

    private final FavoriteTeamNotifier notifier;

    private final com.hooppicks.backendapplication.monitor.JobMonitor jobMonitor;

    public FavoriteTeamNotifierScheduler(FavoriteTeamNotifier notifier, com.hooppicks.backendapplication.monitor.JobMonitor jobMonitor) {
        this.jobMonitor = jobMonitor;
        this.notifier = notifier;
    }

    // Toutes les 5 min, comme la synchro des matchs dont dépendent statut et score.
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void run() {
        Instant now = Instant.now();
        // Deux phases isolées : un souci sur l'une ne prive pas l'autre.
        try {
            int sent = notifier.notifyUpcomingKickoffs(now);
            if (sent > 0) log.info("{} notification(s) « ton équipe joue bientôt » envoyée(s)", sent);
        } catch (Exception e) {
            log.warn("Notifications de coup d'envoi (équipe favorite) échouées", e);
            Sentry.captureException(e);
        }
        try {
            int sent = notifier.notifyResults(now);
            if (sent > 0) log.info("{} notification(s) de résultat (équipe favorite) envoyée(s)", sent);
            jobMonitor.ok("equipe-favorite", null);
        } catch (Exception e) {
            jobMonitor.failed("equipe-favorite", e);
            log.warn("Notifications de résultat (équipe favorite) échouées", e);
            Sentry.captureException(e);
        }
    }
}
