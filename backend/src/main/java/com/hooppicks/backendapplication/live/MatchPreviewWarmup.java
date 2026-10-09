package com.hooppicks.backendapplication.live;

import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Prépare chaque matin l'avant-match des deux jours à venir : la première
 * lecture du calendrier ESPN d'une équipe prend une dizaine de secondes
 * (~1,5 Mo), mieux vaut que ce ne soit pas un joueur qui l'attende. Un match
 * en échec n'empêche pas les autres. Désactivé en test comme les autres
 * tâches planifiées (jamais d'appel ESPN réel en CI).
 */
@Component
@ConditionalOnProperty(prefix = "nba.sync", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class MatchPreviewWarmup {

    private static final Logger log = LoggerFactory.getLogger(MatchPreviewWarmup.class);

    private final MatchPreviewService previewService;
    private final MatchRepository matchRepository;

    public MatchPreviewWarmup(MatchPreviewService previewService, MatchRepository matchRepository) {
        this.previewService = previewService;
        this.matchRepository = matchRepository;
    }

    // Après les effectifs (6h) et le classement (6h05).
    @Scheduled(cron = "0 15 6 * * *", zone = "GMT")
    public void warmUp() {
        Instant now = Instant.now();
        int done = 0;
        for (Match m : matchRepository.findByStatusAndDateBetween(MatchStatus.SCHEDULED, now, now.plus(Duration.ofDays(2)))) {
            try {
                previewService.preview(m.getId());
                done++;
            } catch (RuntimeException e) {
                log.warn("Avant-match non préparé pour {} : {}", m.getId(), e.getMessage());
            }
        }
        log.info("Avant-match préparé pour {} match(s)", done);
    }
}
