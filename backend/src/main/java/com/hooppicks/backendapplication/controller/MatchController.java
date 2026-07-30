package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.MatchDto;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.repository.MatchRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matches")
public class MatchController {

    private final MatchRepository matchRepository;

    public MatchController(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
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
}