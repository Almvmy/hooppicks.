package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.bet.PlayerPropsService;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Public comme /matches : les paris joueurs se consultent sans compte.
@RestController
public class PlayerPropsController {

    private final PlayerPropsService playerPropsService;
    private final MatchRepository matchRepository;

    public PlayerPropsController(PlayerPropsService playerPropsService, MatchRepository matchRepository) {
        this.playerPropsService = playerPropsService;
        this.matchRepository = matchRepository;
    }

    @GetMapping("/matches/{id}/props")
    public List<PlayerPropsService.PlayerProp> props(@PathVariable String id) {
        return matchRepository.findById(id).map(playerPropsService::propsFor).orElse(List.of());
    }
}
