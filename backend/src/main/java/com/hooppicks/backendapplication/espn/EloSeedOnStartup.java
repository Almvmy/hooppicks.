package com.hooppicks.backendapplication.espn;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Au démarrage : les bilans sont souvent déjà en base, inutile d'attendre la
// synchro du classement de 6h05 pour avoir des cotes différenciées. Même
// interrupteur que les autres tâches de fond (désactivé pendant les tests).
@Component
@ConditionalOnProperty(prefix = "nba.sync", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class EloSeedOnStartup {

    private final EspnStandingsService standingsService;

    public EloSeedOnStartup(EspnStandingsService standingsService) {
        this.standingsService = standingsService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        standingsService.seedEloFromStoredRecords();
    }
}
