package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.PlayerDto;
import com.hooppicks.backendapplication.entity.Player;
import com.hooppicks.backendapplication.repository.PlayerRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint public (comme /matches et /leaderboard) : les effectifs ne sont
 * pas une donnée sensible, pas besoin de session pour les consulter.
 */
@RestController
@RequestMapping("/players")
public class PlayerController {

    private final PlayerRepository playerRepository;

    public PlayerController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @GetMapping
    public List<PlayerDto> getPlayers(
            @RequestParam(required = false) String teamId,
            @RequestParam(required = false) String search
    ) {
        List<Player> players;

        if (teamId != null && !teamId.isBlank()) {
            players = playerRepository.findByTeamIdOrderByLastNameAsc(teamId);
        } else if (search != null && !search.isBlank()) {
            players = playerRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(search, search);
        } else {
            players = playerRepository.findAll();
        }

        return players.stream().map(PlayerDto::from).toList();
    }
}
