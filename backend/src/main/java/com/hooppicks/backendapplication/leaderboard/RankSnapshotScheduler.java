package com.hooppicks.backendapplication.leaderboard;

import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

// Même interrupteur que les autres tâches de fond (NbaSyncScheduler) : pas
// d'écriture en base au démarrage des tests à contexte Spring complet.
@Component
@ConditionalOnProperty(prefix = "nba.sync", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class RankSnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(RankSnapshotScheduler.class);

    private final LeaderboardService leaderboardService;

    public RankSnapshotScheduler(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    // Midi, heure de Paris : entre deux nuits de matchs NBA (qui se jouent
    // vers 1h-6h ici). L'évolution affichée couvre ainsi une nuit de
    // résultats complète, pas une nuit coupée en deux par minuit.
    @Scheduled(cron = "0 0 12 * * *", zone = "Europe/Paris")
    public void dailySnapshot() {
        snapshot(LocalDate.now(LeaderboardService.ZONE));
    }

    // Serveur éteint à midi (redémarrage, déploiement) : rattrape la photo
    // du jour au démarrage, pour que l'évolution de demain existe quand même.
    @EventListener(ApplicationReadyEvent.class)
    public void snapshotIfMissing() {
        LocalDate today = LocalDate.now(LeaderboardService.ZONE);
        if (!leaderboardService.hasSnapshot(today)) snapshot(today);
    }

    private void snapshot(LocalDate date) {
        try {
            leaderboardService.takeSnapshot(date);
            log.info("Photo du classement prise pour le {}", date);
        } catch (Exception e) {
            log.warn("Photo du classement échouée pour le {}", date, e);
            Sentry.captureException(e);
        }
    }
}
