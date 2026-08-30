package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.bet.PickPercentagesService;
import com.hooppicks.backendapplication.dto.MatchDto;
import com.hooppicks.backendapplication.dto.PickPercentagesDto;
import com.hooppicks.backendapplication.dto.PlayerBoxScoreDto;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.PlayerMatchStatRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/matches")
public class MatchController {

    private final MatchRepository matchRepository;
    private final PlayerMatchStatRepository playerMatchStatRepository;
    private final PickPercentagesService pickPercentagesService;

    public MatchController(MatchRepository matchRepository, PlayerMatchStatRepository playerMatchStatRepository,
                            PickPercentagesService pickPercentagesService) {
        this.matchRepository = matchRepository;
        this.playerMatchStatRepository = playerMatchStatRepository;
        this.pickPercentagesService = pickPercentagesService;
    }

    @GetMapping
    public List<MatchDto> getAllMatches() {
        List<Match> matches = matchRepository.findAll();
        Map<String, PickPercentagesDto> percentages = pickPercentagesService.forMatches(
                matches.stream().map(Match::getId).toList());
        return matches.stream()
                .map(m -> MatchDto.from(m, percentages.get(m.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    public MatchDto getMatchById(@PathVariable String id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match introuvable : " + id));
        PickPercentagesDto percentages = pickPercentagesService.forMatches(List.of(id)).get(id);
        return MatchDto.from(match, percentages);
    }

    // Vide (jamais 404) si la feuille de match n'a pas encore été importée :
    // match pas encore terminé, ou pas encore traité par EspnStatsService.
    @GetMapping("/{id}/boxscore")
    public List<PlayerBoxScoreDto> getBoxScore(@PathVariable String id) {
        return playerMatchStatRepository.findByMatchIdOrderByPointsDesc(id).stream()
                .map(PlayerBoxScoreDto::from)
                .toList();
    }
}
