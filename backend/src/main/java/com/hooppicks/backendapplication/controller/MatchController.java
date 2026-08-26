package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.MatchDto;
import com.hooppicks.backendapplication.dto.PlayerBoxScoreDto;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.PlayerMatchStatRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matches")
public class MatchController {

    private final MatchRepository matchRepository;
    private final PlayerMatchStatRepository playerMatchStatRepository;

    public MatchController(MatchRepository matchRepository, PlayerMatchStatRepository playerMatchStatRepository) {
        this.matchRepository = matchRepository;
        this.playerMatchStatRepository = playerMatchStatRepository;
    }

    @GetMapping
    public List<MatchDto> getAllMatches() {
        return matchRepository.findAll().stream()
                .map(MatchDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public MatchDto getMatchById(@PathVariable String id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match introuvable : " + id));
        return MatchDto.from(match);
    }

    // Vide (jamais 404) si la feuille de match n'a pas encore été importée —
    // match pas encore terminé, ou pas encore traité par EspnStatsService.
    @GetMapping("/{id}/boxscore")
    public List<PlayerBoxScoreDto> getBoxScore(@PathVariable String id) {
        return playerMatchStatRepository.findByMatchIdOrderByPointsDesc(id).stream()
                .map(PlayerBoxScoreDto::from)
                .toList();
    }
}