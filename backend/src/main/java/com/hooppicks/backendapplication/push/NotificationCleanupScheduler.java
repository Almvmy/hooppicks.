package com.hooppicks.backendapplication.push;

import com.hooppicks.backendapplication.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Notifications de plus de 60 jours effacées chaque nuit : la cloche n'en
 * montre que 50, et un joueur qui suit des matchs en reçoit des dizaines par
 * semaine. Sans ça, la table grossissait sans fin pour rien.
 */
@Component
public class NotificationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationCleanupScheduler.class);
    static final Duration RETENTION = Duration.ofDays(60);

    private final NotificationRepository notificationRepository;

    public NotificationCleanupScheduler(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Scheduled(cron = "0 30 4 * * *", zone = "GMT")
    public void purge() {
        int deleted = notificationRepository.deleteOlderThan(Instant.now().minus(RETENTION));
        if (deleted > 0) log.info("{} notification(s) de plus de {} jours supprimée(s)", deleted, RETENTION.toDays());
    }
}
