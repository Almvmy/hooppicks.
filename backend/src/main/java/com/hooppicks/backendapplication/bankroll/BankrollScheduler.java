package com.hooppicks.backendapplication.bankroll;

import com.hooppicks.backendapplication.nba.NbaSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Passe les joueurs à la nouvelle semaine de jeu. Vérifie chaque minute
 * plutôt qu'un cron le lundi à midi : rattrape tout seul un serveur éteint
 * à l'heure dite, et un compte créé avant les semaines de jeu.
 *
 * Sous le verrou de la synchro, comme la résolution des paris : un gain
 * crédité pendant la remise à niveau serait sinon perdu ou compté deux fois.
 * Si une synchro tourne, on réessaie à la minute suivante.
 */
@Component
@ConditionalOnProperty(prefix = "nba.sync", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class BankrollScheduler {

    private static final Logger log = LoggerFactory.getLogger(BankrollScheduler.class);

    private final BankrollService bankrollService;
    private final NbaSyncService nbaSyncService;

    public BankrollScheduler(BankrollService bankrollService, NbaSyncService nbaSyncService) {
        this.bankrollService = bankrollService;
        this.nbaSyncService = nbaSyncService;
    }

    @Scheduled(fixedRate = 60 * 1000, initialDelay = 20 * 1000)
    public void rollOver() {
        try {
            if (!bankrollService.rollOverNeeded()) return;
            nbaSyncService.tryRunExclusive(() -> {
                int count = bankrollService.rollOverAll();
                if (count > 0) log.info("Semaine de jeu : solde remis à niveau pour {} joueur(s)", count);
            });
        } catch (Exception e) {
            log.error("Passage à la nouvelle semaine de jeu en échec", e);
        }
    }
}
