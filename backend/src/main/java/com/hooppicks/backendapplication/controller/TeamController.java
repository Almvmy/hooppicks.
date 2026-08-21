package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.TeamRankDto;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.repository.TeamRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Les 30 équipes classées par force Elo (cf. EloService/OddsService, jusqu'ici
 * un signal purement interne au calcul des cotes, jamais exposé). Pas
 * d'endpoint par id : pas de profil d'équipe détaillé côté données NBA
 * (balldontlie gratuit ne donne ni effectif fiable ni classement officiel),
 * donc pas la peine de fragmenter en plusieurs routes pour l'instant.
 */
@RestController
@RequestMapping("/teams")
public class TeamController {

    private final TeamRepository teamRepository;

    public TeamController(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
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
                            i + 1, (int) Math.round(t.getEloRating())
                    );
                })
                .toList();
    }
}
