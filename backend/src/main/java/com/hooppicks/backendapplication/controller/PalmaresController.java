package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.leaderboard.LeaderboardPeriod;
import com.hooppicks.backendapplication.palmares.PalmaresService;
import com.hooppicks.backendapplication.palmares.PhaseChampionService;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/palmares")
public class PalmaresController {

    private final PalmaresService palmaresService;
    private final PhaseChampionService phaseChampionService;
    private final SessionStore sessionStore;

    public PalmaresController(PalmaresService palmaresService, PhaseChampionService phaseChampionService,
                              SessionStore sessionStore) {
        this.phaseChampionService = phaseChampionService;
        this.palmaresService = palmaresService;
        this.sessionStore = sessionStore;
    }

    /**
     * Champion(s) de la semaine passée : public, comme le classement (bandeau
     * du classement, couronne, carte de partage). Avec leagueId : ceux de la ligue.
     * Avec phase=cup|playoffs : champion(s) de la Coupe NBA ou des playoffs de la saison.
     */
    @GetMapping("/champions")
    public ResponseEntity<?> champions(@RequestParam(required = false) String leagueId,
                                       @RequestParam(required = false) String phase) {
        if (phase == null || phase.isBlank()) return ResponseEntity.ok(palmaresService.lastWeekChampions(leagueId));
        LeaderboardPeriod parsed;
        try {
            parsed = LeaderboardPeriod.parse(phase);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        if (parsed != LeaderboardPeriod.CUP && parsed != LeaderboardPeriod.PLAYOFFS) {
            return ResponseEntity.badRequest().body("Phase inconnue : " + phase);
        }
        return ResponseEntity.ok(phaseChampionService.champions(parsed, leagueId));
    }

    @GetMapping("/me")
    public ResponseEntity<?> mine(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(palmaresService.of(userId));
    }
}
