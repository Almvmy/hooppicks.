package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.MatchType;
import com.hooppicks.backendapplication.entity.PlayerMatchStat;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.PlayerMatchStatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

    // Pas de méthode syncEspnData() qui enchaîne les deux ci-dessous en
    // interne : un appel this.linkEventIds() depuis l'intérieur de la classe
    // ne passe pas par le proxy Spring, donc @Transactional n'a aucun effet
    // (auto-invocation, piège classique de Spring AOP) — la session Hibernate
    // se refermait avant que match.getHomeTeam() ne soit lu plus loin dans la
    // boucle (LazyInitializationException depuis le passage de Match.homeTeam/
    // awayTeam en LAZY). NbaSyncScheduler appelle donc chaque méthode
    // directement, en tant qu'appelant externe, pour que le proxy s'applique.
    /**
     * Relie les matchs à leur event ESPN et lit leur phase (présaison, Coupe
     * NBA, playoffs…), par petits lots. Passe de rattrapage : les matchs de
     * la fenêtre de synchro sont déjà traités à chaque tick par
     * EspnScheduleService ; ici on reprend les autres (anciens matchs,
     * matchs créés avant l'ajout de la phase).
     */
    @Transactional
    public void linkEventIds() {
        List<Match> matches = matchRepository.findByTypeIsNullOrderByDateDesc(PageRequest.of(0, BATCH_SIZE));
        for (Match match : matches) {
            if (match.getHomeTeam() == null || match.getAwayTeam() == null || match.getDate() == null) continue;

            try {
                // Journée ESPN = journée à l'heure de New York : en UTC, un
                // match à 22h (côte Est) tombe déjà le lendemain et n'était
                // jamais retrouvé.
                Optional<List<EspnGameRow>> rows = espnStatsClient.fetchScoreboard(
                        match.getDate().atZone(EspnScheduleService.NBA_ZONE).toLocalDate());
                if (rows.isEmpty()) continue; // ESPN injoignable : retenté au prochain tick

                String home = match.getHomeTeam().getAbbreviation();
                String away = match.getAwayTeam().getAbbreviation();
                Optional<EspnGameRow> row = rows.get().stream()
                        .filter(r -> match.getEspnEventId() != null
                                ? match.getEspnEventId().equals(r.eventId())
                                : home.equals(r.homeAbbreviation()) && away.equals(r.awayAbbreviation()))
                        .findFirst();
                if (row.isPresent()) {
                    match.setEspnEventId(row.get().eventId());
                    EspnMatchStage.of(row.get()).applyTo(match);
                } else {
                    // Introuvable sur ESPN (qui publie pourtant tout le
                    // calendrier à l'avance) : saison régulière par défaut,
                    // pour ne pas le réessayer à chaque tick et bloquer le lot.
                    match.setType(MatchType.REGULAR);
                }
                matchRepository.save(match);
            } catch (Exception e) {
                // Un match qu'on n'arrive pas à relier à ESPN reste juste sans
                // feuille de match ni phase : jamais bloquant pour le reste de l'app.
                log.warn("Liaison ESPN échouée pour le match {} : {}", match.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void importBoxScores() {
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
