package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.RosterPlayerDto;
import com.hooppicks.backendapplication.dto.TeamRankDto;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.repository.RosterPlayerRepository;
import com.hooppicks.backendapplication.repository.TeamRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Les 30 équipes classées par force Elo (cf. EloService/OddsService, jusqu'ici
 * un signal purement interne au calcul des cotes, jamais exposé), plus
 * l'effectif actuel de chaque équipe (cf. EspnRosterService/RosterPlayer) :
 * balldontlie gratuit ne donnait ni l'un ni l'autre de façon fiable.
 */
@RestController
@RequestMapping("/teams")
public class TeamController {

    private final TeamRepository teamRepository;
    private final RosterPlayerRepository rosterPlayerRepository;
    private final com.hooppicks.backendapplication.power.PowerRankingService powerRankingService;
    private final com.hooppicks.backendapplication.espn.TeamScheduleService teamScheduleService;

    public TeamController(TeamRepository teamRepository, RosterPlayerRepository rosterPlayerRepository,
                          com.hooppicks.backendapplication.power.PowerRankingService powerRankingService,
                          com.hooppicks.backendapplication.espn.TeamScheduleService teamScheduleService) {
        this.teamScheduleService = teamScheduleService;
        this.teamRepository = teamRepository;
        this.rosterPlayerRepository = rosterPlayerRepository;
        this.powerRankingService = powerRankingService;
    }

    @GetMapping
    public List<TeamRankDto> getTeamRankings() {
        List<Team> teams = teamRepository.findAll().stream()
                .sorted(Comparator.comparingDouble(Team::getEloRating).reversed())
                .toList();

        return java.util.stream.IntStream.range(0, teams.size())
                .mapToObj(i -> {
                    Team t = teams.get(i);
                    return new TeamRankDto(
                            t.getId(), t.getName(), t.getAbbreviation(),
                            t.getConference(), t.getDivision(),
                            i + 1, (int) Math.round(t.getEloRating()),
                            t.getWins(), t.getLosses(), t.getStreak(),
                            t.getConferenceSeed(), t.getGamesBehind(), t.getLogoUrl()
                    );
                })
                .toList();
    }

    /** Power rankings HoopPicks (public) : rang Elo et évolution sur 7 jours. */
    @GetMapping("/power-rankings")
    public List<com.hooppicks.backendapplication.power.PowerRankingService.Entry> powerRankings() {
        return powerRankingService.rankings();
    }

    /** Calendrier de saison régulière d'une équipe (public), par sigle : sert l'abonnement agenda. */
    @GetMapping("/{abbreviation}/schedule")
    public org.springframework.http.ResponseEntity<List<com.hooppicks.backendapplication.espn.EspnStatsClient.ScheduledGame>> schedule(
            @PathVariable String abbreviation) {
        String abbr = abbreviation.toUpperCase(java.util.Locale.ROOT);
        if (teamRepository.findAll().stream().noneMatch(t -> t.getAbbreviation().equals(abbr))) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        return teamScheduleService.schedule(abbr).map(org.springframework.http.ResponseEntity::ok)
                .orElseGet(() -> org.springframework.http.ResponseEntity.status(503).build());
    }

    @GetMapping("/{id}/roster")
    public List<RosterPlayerDto> getRoster(@PathVariable String id) {
        return rosterPlayerRepository.findByTeamIdOrderByLastNameAsc(id).stream()
                .map(RosterPlayerDto::from)
                .toList();
    }
}
