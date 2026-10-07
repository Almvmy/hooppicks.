package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Classement officiel (victoires/défaites) + logo d'équipe, séparé de l'Elo
 * interne (cf. commentaire sur Team.wins) : un seul appel ESPN pour toute la
 * ligue, donc pas besoin d'un traitement par lot comme EspnRosterService/
 * EspnStatsService. Le logo n'a rien à voir avec le classement, mais on le
 * récupère ici en passant : c'est le seul appel qui renvoie déjà les 30
 * équipes avec cette donnée, pas la peine d'un appel dédié.
 */
@Service
public class EspnStandingsService {

    private static final Logger log = LoggerFactory.getLogger(EspnStandingsService.class);

    private final EspnStatsClient espnStatsClient;
    private final TeamRepository teamRepository;

    public EspnStandingsService(EspnStatsClient espnStatsClient, TeamRepository teamRepository,
                                com.hooppicks.backendapplication.nba.EloService eloService) {
        this.eloService = eloService;
        this.espnStatsClient = espnStatsClient;
        this.teamRepository = teamRepository;
    }

    private final com.hooppicks.backendapplication.nba.EloService eloService;

    /** Amorce l'Elo des équipes dont le bilan est déjà en base (sans appel ESPN). */
    @Transactional
    public int seedEloFromStoredRecords() {
        int seeded = 0;
        for (Team team : teamRepository.findAll()) {
            if (eloService.seedFromRecord(team)) {
                teamRepository.save(team);
                seeded++;
            }
        }
        if (seeded > 0) log.info("Elo de départ amorcé pour {} équipe(s) à partir de leur bilan", seeded);
        return seeded;
    }

    @Scheduled(cron = "0 5 6 * * *")
    @Transactional
    public void syncStandings() {
        List<EspnStandingRow> rows = espnStatsClient.fetchStandings();
        if (rows.isEmpty()) {
            log.warn("Synchro classement ESPN : réponse vide, on garde l'ancien classement.");
            return;
        }

        Map<String, Team> teamsByEspnAbbr = teamRepository.findAll().stream()
                .collect(Collectors.toMap(t -> EspnStatsClient.toEspnAbbreviation(t.getAbbreviation()), t -> t));

        int updated = 0;
        for (EspnStandingRow row : rows) {
            Team team = teamsByEspnAbbr.get(row.teamAbbreviation());
            if (team == null) continue; // sigle ESPN sans correspondance locale (ne devrait pas arriver pour les 30 équipes)

            team.setWins(row.wins());
            team.setLosses(row.losses());
            team.setStreak(row.streak());
            team.setConferenceSeed(row.conferenceSeed());
            team.setGamesBehind(row.gamesBehind());
            team.setLogoUrl(row.logoUrl());
            // Avant le premier match de la saison, ESPN renvoie encore le bilan
            // de la saison passée : c'est lui qui sert d'Elo de départ.
            eloService.seedFromRecord(team);
            teamRepository.save(team);
            updated++;
        }
        log.info("Classement ESPN synchronisé ({} équipes)", updated);
    }
}
