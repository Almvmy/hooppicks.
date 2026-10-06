package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.bet.BetResolutionService;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.MatchType;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.nba.NbaSyncService;
import com.hooppicks.backendapplication.nba.OddsService;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ESPN comme source de matchs secondaire, sur la même fenêtre de dates que
 * la synchro balldontlie :
 * - relie chaque match connu à son event ESPN et lit sa phase (Coupe NBA,
 *   playoffs, état de la série…) ;
 * - crée les matchs de présaison, que balldontlie ne connaît pas du tout, et
 *   les tient à jour (statut, score) pour que leurs paris se résolvent.
 *
 * balldontlie reste la source de référence : un match qu'il fournit n'est
 * jamais créé ici, et son statut/score n'est jamais touché par ESPN. Si ESPN
 * tombe, seuls la présaison et les intitulés de phase cessent d'avancer ; la
 * synchro balldontlie, appelée avant et isolée dans sa propre phase du
 * scheduler, n'en dépend en rien.
 */
@Service
public class EspnScheduleService {

    private static final Logger log = LoggerFactory.getLogger(EspnScheduleService.class);

    /** Le calendrier NBA (et ESPN) compte ses journées à l'heure de la côte Est. */
    public static final ZoneId NBA_ZONE = ZoneId.of("America/New_York");

    // Écart toléré entre l'heure ESPN et la nôtre pour reconnaître le même
    // match (horaire modifié après coup d'un côté seulement).
    private static final Duration SAME_GAME_TOLERANCE = Duration.ofHours(12);

    private final EspnStatsClient espnStatsClient;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final BetRepository betRepository;
    private final OddsService oddsService;
    private final NbaSyncService nbaSyncService;
    private final BetResolutionService betResolutionService;
    private final TransactionTemplate transactionTemplate;

    public EspnScheduleService(EspnStatsClient espnStatsClient, MatchRepository matchRepository,
                               TeamRepository teamRepository, BetRepository betRepository,
                               OddsService oddsService, NbaSyncService nbaSyncService,
                               BetResolutionService betResolutionService,
                               PlatformTransactionManager transactionManager) {
        this.espnStatsClient = espnStatsClient;
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.betRepository = betRepository;
        this.oddsService = oddsService;
        this.nbaSyncService = nbaSyncService;
        this.betResolutionService = betResolutionService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** Nombre de matchs mis à jour, ou 0 si ESPN ne répond pas. */
    public int syncWindow(List<LocalDate> dates) {
        // Appels réseau d'abord, hors transaction : on ne garde pas une
        // connexion base ouverte pendant une série d'appels ESPN.
        List<EspnGameRow> rows = new ArrayList<>();
        for (LocalDate date : dates) {
            Optional<List<EspnGameRow>> day = espnStatsClient.fetchScoreboard(date);
            if (day.isEmpty()) {
                // Inutile d'enchaîner les autres jours (chacun coûterait un
                // retry de plus) : on réessaiera au prochain tick.
                log.warn("ESPN injoignable, synchro des matchs ESPN reportée au prochain tick");
                return 0;
            }
            rows.addAll(day.get());
        }
        if (rows.isEmpty()) return 0;

        int[] updated = {0};
        boolean ran = nbaSyncService.tryRunExclusive(() ->
                transactionTemplate.executeWithoutResult(status -> updated[0] = apply(rows)));
        if (!ran) log.info("Synchro balldontlie en cours, synchro ESPN des matchs reportée au prochain tick");
        return updated[0];
    }

    int apply(List<EspnGameRow> rows) {
        Map<String, Team> teams = teamRepository.findAll().stream()
                .collect(Collectors.toMap(Team::getAbbreviation, Function.identity(), (a, b) -> a));
        int updated = 0;
        boolean anyFinished = false;

        for (EspnGameRow row : rows) {
            Team home = teams.get(row.homeAbbreviation());
            Team away = teams.get(row.awayAbbreviation());
            // Club étranger en tournée de présaison, équipes de l'All-Star
            // Game ("Stars", "World"…) : pas des franchises, pas de pari possible.
            if (home == null || away == null || row.date() == null) continue;

            Match match = matchRepository.findFirstByEspnEventId(row.eventId())
                    .or(() -> matchRepository.findFirstByHomeTeamAndAwayTeamAndDateBetween(
                            home, away, row.date().minus(SAME_GAME_TOLERANCE), row.date().plus(SAME_GAME_TOLERANCE)))
                    .orElse(null);
            EspnMatchStage stage = EspnMatchStage.of(row);

            if (match == null) {
                // Seule la présaison est créée depuis ESPN : tout le reste
                // arrive par balldontlie, et sera relié au tick suivant.
                if (stage.type() != MatchType.PRESEASON) continue;
                match = new Match();
                match.setHomeTeam(home);
                match.setAwayTeam(away);
            }

            match.setEspnEventId(row.eventId());
            stage.applyTo(match);

            // Corrigé à la main (console admin) : ESPN ne touche plus au statut ni au score.
            if (match.getExternalId() == null && !Boolean.TRUE.equals(match.getAdminLocked())) {
                MatchStatus previous = match.getStatus();
                updateFromEspn(match, row, home, away);
                matchRepository.save(match);
                if (previous == MatchStatus.SCHEDULED && match.getStatus() == MatchStatus.LIVE) {
                    nbaSyncService.notifyMatchStarting(match);
                }
                anyFinished |= previous != MatchStatus.FINISHED && match.getStatus() == MatchStatus.FINISHED;
                // Pas d'Elo pour la présaison : les titulaires y jouent peu,
                // ses résultats fausseraient les cotes de la saison.
            } else {
                matchRepository.save(match);
            }
            updated++;
        }

        if (anyFinished) betResolutionService.resolvePendingBets();
        return updated;
    }

    // Match dont ESPN est la seule source (présaison) : statut, score, horaire
    // et cotes suivent ESPN, avec la même règle de cotes que NbaSyncService.
    private void updateFromEspn(Match match, EspnGameRow row, Team home, Team away) {
        boolean isNew = match.getId() == null;
        MatchStatus status = row.matchStatus();
        match.setDate(row.date());
        match.setStatus(status);
        if (status != MatchStatus.SCHEDULED) {
            match.setHomeScore(row.homeScore());
            match.setAwayScore(row.awayScore());
        }
        if (isNew || (status == MatchStatus.SCHEDULED && !betRepository.existsPendingBetForMatch(match.getId()))) {
            oddsService.applyOdds(match, home, away);
        }
    }
}
