package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.leaderboard.LeaderboardPeriod;
import com.hooppicks.backendapplication.leaderboard.LeaderboardService;
import com.hooppicks.backendapplication.palmares.PhaseChampionService;
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
    private final PhaseChampionService phaseChampionService;
    private final SessionStore sessionStore;

    public LeaderboardController(LeaderboardService leaderboardService, PhaseChampionService phaseChampionService,
                                 SessionStore sessionStore) {
        this.phaseChampionService = phaseChampionService;
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

    /** Classements Coupe NBA et playoffs affichés (phase commencée cette saison). */
    @GetMapping("/phases")
    public ResponseEntity<?> getPhases() {
        return ResponseEntity.ok(phaseChampionService.openPhases());
    }

    /** period : season (défaut), month, week, cup ou playoffs. */
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
