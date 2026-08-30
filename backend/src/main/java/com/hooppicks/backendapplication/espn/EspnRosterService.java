package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.entity.RosterPlayer;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.repository.RosterPlayerRepository;
import com.hooppicks.backendapplication.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Synchro des effectifs ESPN, séparée de NbaSyncScheduler (5 min) : un
 * roster NBA ne change pas d'une minute à l'autre comme un score, une
 * fois par jour suffit largement. 30 équipes, petits payloads : pas le
 * même risque mémoire que l'import des feuilles de match (EspnStatsService).
 *
 * Upsert par id ESPN plutôt que delete-and-recreate : un joueur qui reste
 * dans l'effectif garde ses moyennes saison déjà synchronisées (cf.
 * EspnPlayerStatsService) au lieu de les perdre à chaque passage quotidien.
 * Seuls les joueurs qui ne sont plus dans la réponse ESPN (trade, coupure)
 * sont supprimés.
 */
@Service
public class EspnRosterService {

    private static final Logger log = LoggerFactory.getLogger(EspnRosterService.class);

    // ESPN renvoie les dates de blessure sans les secondes (ex.
    // "2026-07-27T16:11Z"), qu'Instant.parse() rejette tel quel : vérifié en
    // direct, pas une supposition.
    private static final DateTimeFormatter INJURY_DATE_FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart().appendPattern(":ss").optionalEnd()
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .appendOffset("+HH:MM", "Z")
            .toFormatter();

    private final EspnStatsClient espnStatsClient;
    private final TeamRepository teamRepository;
    private final RosterPlayerRepository rosterPlayerRepository;

    public EspnRosterService(EspnStatsClient espnStatsClient, TeamRepository teamRepository,
                              RosterPlayerRepository rosterPlayerRepository) {
        this.espnStatsClient = espnStatsClient;
        this.teamRepository = teamRepository;
        this.rosterPlayerRepository = rosterPlayerRepository;
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void syncRosters() {
        List<Team> teams = teamRepository.findAll();
        for (Team team : teams) {
            syncTeamRoster(team);
        }
        log.info("Rosters ESPN synchronisés ({} équipes)", teams.size());
    }

    @Transactional
    void syncTeamRoster(Team team) {
        try {
            List<EspnRosterRow> rows = espnStatsClient.fetchRoster(team.getAbbreviation());
            // Liste vide = probablement un échec réseau silencieux (voir
            // fetchWithRetry) plutôt qu'un vrai roster à zéro joueur : on
            // garde l'ancien roster plutôt que de le vider pour rien.
            if (rows.isEmpty()) return;

            Set<String> freshIds = rows.stream()
                    .map(EspnRosterRow::espnId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            for (RosterPlayer existing : rosterPlayerRepository.findByTeamIdOrderByLastNameAsc(team.getId())) {
                if (!freshIds.contains(existing.getId())) {
                    rosterPlayerRepository.delete(existing);
                }
            }

            int outCount = 0;
            for (EspnRosterRow row : rows) {
                if (row.espnId() == null) continue;

                RosterPlayer player = rosterPlayerRepository.findById(row.espnId()).orElseGet(RosterPlayer::new);
                player.setId(row.espnId());
                player.setTeam(team);
                player.setFirstName(row.firstName());
                player.setLastName(row.lastName());
                player.setPosition(row.position());
                player.setJersey(row.jersey());
                player.setHeight(row.height());
                player.setWeight(row.weight());
                player.setHeadshotUrl(row.headshotUrl());
                player.setInjuryStatus(row.injuryStatus());
                player.setInjuryDate(parseInjuryDate(row.injuryDate()));
                rosterPlayerRepository.save(player);

                if ("Out".equals(row.injuryStatus())) outCount++;
            }
            team.setOutPlayersCount(outCount);
            teamRepository.save(team);
        } catch (Exception e) {
            log.warn("Synchro roster ESPN échouée pour {} : {}", team.getAbbreviation(), e.getMessage());
        }
    }

    private Instant parseInjuryDate(String raw) {
        if (raw == null) return null;
        try {
            return Instant.from(INJURY_DATE_FORMAT.parse(raw));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
