package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.PlayerMatchStat;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.PlayerMatchStatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;

/**
 * Enrichissement ESPN (ID d'event + feuille de match), volontairement tenu à
 * l'écart de la grosse transaction de NbaSyncService.doSyncGames : chaque
 * appel ESPN peut prendre jusqu'à ~22s en cas de retry, et le lot pourrait
 * grossir sans borne si on le laissait tourner dans la même boucle : c'est
 * exactement ce type de traitement en un seul gros paquet qui avait fait
 * dépasser la mémoire allouée sur Railway. Ici, un nombre fixe et petit de
 * matchs traités par appel, quel que soit le nombre de matchs en attente.
 */
@Service
public class EspnStatsService {

    private static final Logger log = LoggerFactory.getLogger(EspnStatsService.class);
    private static final int BATCH_SIZE = 3;

    private final EspnStatsClient espnStatsClient;
    private final MatchRepository matchRepository;
    private final PlayerMatchStatRepository playerMatchStatRepository;

    public EspnStatsService(EspnStatsClient espnStatsClient, MatchRepository matchRepository,
                             PlayerMatchStatRepository playerMatchStatRepository) {
        this.espnStatsClient = espnStatsClient;
        this.matchRepository = matchRepository;
        this.playerMatchStatRepository = playerMatchStatRepository;
    }

    public void syncEspnData() {
        linkEventIds();
        importBoxScores();
    }

    @Transactional
    void linkEventIds() {
        List<Match> matches = matchRepository.findByEspnEventIdIsNull(PageRequest.of(0, BATCH_SIZE));
        for (Match match : matches) {
            if (match.getHomeTeam() == null || match.getAwayTeam() == null || match.getDate() == null) continue;

            try {
                espnStatsClient.findEventId(
                        match.getDate().atZone(ZoneOffset.UTC).toLocalDate(),
                        match.getHomeTeam().getAbbreviation(),
                        match.getAwayTeam().getAbbreviation()
                ).ifPresent(eventId -> {
                    match.setEspnEventId(eventId);
                    matchRepository.save(match);
                });
            } catch (Exception e) {
                // Un match qu'on n'arrive pas à relier à ESPN reste juste sans
                // feuille de match : jamais bloquant pour le reste de l'app.
                log.warn("Liaison ESPN échouée pour le match {} : {}", match.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    void importBoxScores() {
        List<Match> matches = matchRepository.findFinishedWithoutBoxScore(
                MatchStatus.FINISHED, PageRequest.of(0, BATCH_SIZE));

        for (Match match : matches) {
            try {
                List<PlayerBoxScoreRow> rows = espnStatsClient.fetchBoxScore(match.getEspnEventId());
                for (PlayerBoxScoreRow row : rows) {
                    PlayerMatchStat stat = new PlayerMatchStat();
                    stat.setMatch(match);
                    stat.setPlayerName(row.playerName());
                    stat.setTeamAbbreviation(row.teamAbbreviation());
                    stat.setStarter(row.starter());
                    stat.setMinutes(row.minutes());
                    stat.setPoints(row.points());
                    stat.setRebounds(row.rebounds());
                    stat.setAssists(row.assists());
                    stat.setSteals(row.steals());
                    stat.setBlocks(row.blocks());
                    stat.setTurnovers(row.turnovers());
                    stat.setPlusMinus(row.plusMinus());
                    stat.setFieldGoalsMade(row.fieldGoals().made());
                    stat.setFieldGoalsAttempted(row.fieldGoals().attempted());
                    stat.setThreePointsMade(row.threePoints().made());
                    stat.setThreePointsAttempted(row.threePoints().attempted());
                    stat.setFreeThrowsMade(row.freeThrows().made());
                    stat.setFreeThrowsAttempted(row.freeThrows().attempted());
                    playerMatchStatRepository.save(stat);
                }
                if (!rows.isEmpty()) {
                    log.info("Feuille de match importée pour {} ({} lignes)", match.getId(), rows.size());
                }
            } catch (Exception e) {
                log.warn("Import de la feuille de match échoué pour le match {} : {}", match.getId(), e.getMessage());
            }
        }
    }
}
