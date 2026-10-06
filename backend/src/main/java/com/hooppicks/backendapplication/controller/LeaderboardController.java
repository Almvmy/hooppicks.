package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.leaderboard.LeaderboardPeriod;
import com.hooppicks.backendapplication.leaderboard.LeaderboardService;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final SessionStore sessionStore;

    public LeaderboardController(LeaderboardService leaderboardService, SessionStore sessionStore) {
        this.leaderboardService = leaderboardService;
        this.sessionStore = sessionStore;
    }

    /** Score et rang du joueur connecté (saison + semaine de jeu). */
    @GetMapping("/me")
    public ResponseEntity<?> getMyStanding(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(leaderboardService.standing(userId));
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
