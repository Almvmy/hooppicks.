package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.leaderboard.LeaderboardPeriod;
import com.hooppicks.backendapplication.leaderboard.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /** period : season (défaut), month ou week. */
    @GetMapping
    public ResponseEntity<?> getLeaderboard(@RequestParam(required = false) String period) {
        LeaderboardPeriod parsed;
        try {
            parsed = LeaderboardPeriod.parse(period);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok(leaderboardService.leaderboard(parsed));
    }
}
