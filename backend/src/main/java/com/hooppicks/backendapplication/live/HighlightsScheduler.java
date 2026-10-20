package com.hooppicks.backendapplication.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Toutes les 10 minutes : le flux de la chaîne NBA ne garde que ses 15 dernières vidéos. Coupé en test, comme la synchro. */
@Component
@ConditionalOnProperty(prefix = "nba.sync", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class HighlightsScheduler {

    private static final Logger log = LoggerFactory.getLogger(HighlightsScheduler.class);

    private final HighlightsService highlightsService;

    public HighlightsScheduler(HighlightsService highlightsService) {
        this.highlightsService = highlightsService;
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000, initialDelay = 2 * 60 * 1000)
    public void refresh() {
        try {
            highlightsService.refresh();
        } catch (Exception e) {
            log.error("Résumés vidéo en échec", e);
        }
    }
}
