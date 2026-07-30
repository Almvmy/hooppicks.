package com.hooppicks.backendapplication.nba;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.LongStream;

@Component
public class NbaSyncScheduler {

    private final NbaSyncService nbaSyncService;

    @Value("${nba.sync.use-fixed-window:false}")
    private boolean useFixedWindow;

    @Value("${nba.sync.window-start:}")
    private String windowStart;

    @Value("${nba.sync.window-days:10}")
    private int windowDays;

    public NbaSyncScheduler(NbaSyncService nbaSyncService) {
        this.nbaSyncService = nbaSyncService;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000) // toutes les 5 minutes
    public void refreshRecentGames() {
        List<LocalDate> dates;

        if (useFixedWindow && !windowStart.isBlank()) {
            // Mode "intersaison" : on cible une période passée qui a eu de vrais matchs
            // (ex: playoffs de juin), pour continuer à voir des données vivantes pendant qu'on développe.
            LocalDate start = LocalDate.parse(windowStart);
            dates = LongStream.rangeClosed(0, windowDays).mapToObj(start::plusDays).toList();
        } else {
            // Mode normal (saison en cours) : hier/aujourd'hui/demain.
            LocalDate today = LocalDate.now();
            dates = List.of(today.minusDays(1), today, today.plusDays(1));
        }

        int updated = nbaSyncService.syncGames(dates);
        System.out.println("[NbaSyncScheduler] " + updated + " match(s) synchronisé(s) (fenêtre "
                + (useFixedWindow ? "fixe " + windowStart : "glissante") + ")");
    }
}