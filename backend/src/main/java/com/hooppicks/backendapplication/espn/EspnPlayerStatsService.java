package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.entity.RosterPlayer;
import com.hooppicks.backendapplication.repository.RosterPlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Moyennes saison par joueur — un appel ESPN par joueur (~550 au total),
 * donc traité par petits lots à chaque tick du scheduler principal
 * (NbaSyncScheduler, seul appelant) plutôt que d'un coup — même précaution
 * mémoire que EspnStatsService pour les feuilles de match. Pas de @Scheduled
 * ici volontairement : appelé depuis NbaSyncScheduler pour hériter de son
 * garde-fou de test (nba.sync.scheduler-enabled=false), sinon ce lot partirait
 * dès le démarrage du contexte pendant les tests. Le tri "jamais synchronisé
 * puis plus ancien d'abord" de RosterPlayerRepository fait qu'un premier
 * passage complet prend plusieurs heures, puis le cycle recommence en boucle
 * pour garder les moyennes à jour au fil de la saison.
 */
@Service
public class EspnPlayerStatsService {

    private static final Logger log = LoggerFactory.getLogger(EspnPlayerStatsService.class);
    private static final int BATCH_SIZE = 8;

    private final EspnStatsClient espnStatsClient;
    private final RosterPlayerRepository rosterPlayerRepository;

    public EspnPlayerStatsService(EspnStatsClient espnStatsClient, RosterPlayerRepository rosterPlayerRepository) {
        this.espnStatsClient = espnStatsClient;
        this.rosterPlayerRepository = rosterPlayerRepository;
    }

    @Transactional
    public void syncBatch() {
        List<RosterPlayer> batch = rosterPlayerRepository.findAllOrderByStatsUpdatedAtAscNullsFirst(
                PageRequest.of(0, BATCH_SIZE));

        int updated = 0;
        for (RosterPlayer player : batch) {
            try {
                Optional<PlayerSeasonStatsRow> statsRow = espnStatsClient.fetchSeasonStats(player.getId());
                if (statsRow.isPresent()) {
                    apply(player, statsRow.get());
                    updated++;
                }
                // Même en cas d'échec/absence de données, on marque le passage
                // pour ne pas bloquer le lot suivant sur ce même joueur en boucle.
                player.setStatsUpdatedAt(Instant.now());
                rosterPlayerRepository.save(player);
            } catch (Exception e) {
                log.warn("Stats saison ESPN échouées pour le joueur {} : {}", player.getId(), e.getMessage());
            }
        }
        if (updated > 0) {
            log.info("Stats saison ESPN synchronisées pour {} joueur(s)", updated);
        }
    }

    private void apply(RosterPlayer player, PlayerSeasonStatsRow row) {
        player.setStatsSeasonLabel(row.seasonLabel());
        player.setGamesPlayed(row.gamesPlayed());
        player.setGamesStarted(row.gamesStarted());
        player.setMinutesPerGame(row.minutesPerGame());
        player.setPointsPerGame(row.pointsPerGame());
        player.setReboundsPerGame(row.reboundsPerGame());
        player.setAssistsPerGame(row.assistsPerGame());
        player.setStealsPerGame(row.stealsPerGame());
        player.setBlocksPerGame(row.blocksPerGame());
        player.setTurnoversPerGame(row.turnoversPerGame());
        player.setFieldGoalPct(row.fieldGoalPct());
        player.setThreePointPct(row.threePointPct());
        player.setFreeThrowPct(row.freeThrowPct());
    }
}
