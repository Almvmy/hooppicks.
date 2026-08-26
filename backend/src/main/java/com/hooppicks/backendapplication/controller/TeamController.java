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
 * l'effectif actuel de chaque équipe (cf. EspnRosterService/RosterPlayer) —
 * balldontlie gratuit ne donnait ni l'un ni l'autre de façon fiable.
 */
@RestController
@RequestMapping("/teams")
public class TeamController {

    private final TeamRepository teamRepository;
    private final RosterPlayerRepository rosterPlayerRepository;

    public TeamController(TeamRepository teamRepository, RosterPlayerRepository rosterPlayerRepository) {
        this.teamRepository = teamRepository;
        this.rosterPlayerRepository = rosterPlayerRepository;
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

    @GetMapping("/{id}/roster")
    public List<RosterPlayerDto> getRoster(@PathVariable String id) {
        return rosterPlayerRepository.findByTeamIdOrderByLastNameAsc(id).stream()
                .map(RosterPlayerDto::from)
                .toList();
    }
}
