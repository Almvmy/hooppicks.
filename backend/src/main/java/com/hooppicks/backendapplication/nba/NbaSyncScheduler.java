package com.hooppicks.backendapplication.nba;

import com.hooppicks.backendapplication.espn.EspnPlayerStatsService;
import com.hooppicks.backendapplication.espn.EspnStatsService;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.LongStream;

// Désactivable (nba.sync.scheduler-enabled=false) : les tests avec contexte
// Spring complet (@SpringBootTest) déclenchaient sinon ce scheduler pour de
// vrai au démarrage, avec de vrais appels réseau (balldontlie + ESPN) :
// lent et fragile en test, voir application-test.properties.
@Component
@ConditionalOnProperty(prefix = "nba.sync", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class NbaSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(NbaSyncScheduler.class);

    private final NbaSyncService nbaSyncService;
    private final AdminSyncStatus adminSyncStatus;
    private final EspnStatsService espnStatsService;
    private final EspnPlayerStatsService espnPlayerStatsService;

    @Value("${nba.sync.use-fixed-window:false}")
    private boolean useFixedWindow;

    @Value("${nba.sync.window-start:}")
    private String windowStart;

    @Value("${nba.sync.window-days:10}")
    private int windowDays;

    public NbaSyncScheduler(NbaSyncService nbaSyncService, AdminSyncStatus adminSyncStatus,
                             EspnStatsService espnStatsService, EspnPlayerStatsService espnPlayerStatsService) {
        this.nbaSyncService = nbaSyncService;
        this.adminSyncStatus = adminSyncStatus;
        this.espnStatsService = espnStatsService;
        this.espnPlayerStatsService = espnPlayerStatsService;
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

        String mode = useFixedWindow ? "fixe " + windowStart : "glissante";

        // Chaque phase isolée dans son propre try/catch : ce sont trois
        // intégrations externes indépendantes (balldontlie, puis deux appels
        // ESPN), sans lien de dépendance entre elles. Avant ce correctif, une
        // erreur balldontlie (ex. 429 quota dépassé) faisait remonter
        // l'exception hors de cette méthode et annulait silencieusement les
        // deux phases ESPN suivantes pour tout le tick : un souci sur l'une
        // n'a aucune raison de priver les deux autres de leur rafraîchissement.
        try {
            NbaSyncService.SyncResult result = nbaSyncService.syncGames(dates);
            adminSyncStatus.recordSync(result.gamesSynced(), result.betsResolved(), mode);
            log.info("{} match(s) synchronisé(s), {} pari(s) résolu(s) (fenêtre {})",
                    result.gamesSynced(), result.betsResolved(), mode);
        } catch (Exception e) {
            log.warn("Synchro balldontlie (matchs) échouée, tick ignoré pour cette phase", e);
            Sentry.captureException(e);
        }

        // Après la synchro balldontlie, pas dedans : voir EspnStatsService
        // pour pourquoi ce doit être un traitement séparé et borné. Les deux
        // méthodes sont appelées ici directement (pas via un wrapper interne
        // à EspnStatsService) pour que leur @Transactional respectif
        // s'applique réellement : un appel depuis l'intérieur de la classe ne
        // passe pas par le proxy Spring (cf. commentaire sur linkEventIds).
        try {
            espnStatsService.linkEventIds();
        } catch (Exception e) {
            log.warn("Synchro ESPN (liaison des ids d'event) échouée, tick ignoré pour cette phase", e);
            Sentry.captureException(e);
        }
        try {
            espnStatsService.importBoxScores();
        } catch (Exception e) {
            log.warn("Synchro ESPN (import des feuilles de match) échouée, tick ignoré pour cette phase", e);
            Sentry.captureException(e);
        }
        try {
            espnPlayerStatsService.syncBatch();
        } catch (Exception e) {
            log.warn("Synchro ESPN (stats joueurs par lot) échouée, tick ignoré pour cette phase", e);
            Sentry.captureException(e);
        }
    }
}