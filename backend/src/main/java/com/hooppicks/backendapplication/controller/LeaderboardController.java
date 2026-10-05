package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.LeaderboardEntryDto;
import com.hooppicks.backendapplication.repository.BetRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final BetRepository betRepository;

    public LeaderboardController(BetRepository betRepository) {
        this.betRepository = betRepository;
    }

    @GetMapping
    public List<LeaderboardEntryDto> getLeaderboard() {
        return LeaderboardEntryDto.fromRows(betRepository.getLeaderboardRaw());
    }
}
