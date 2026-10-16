package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.palmares.PalmaresService;
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
    private final SessionStore sessionStore;

    public PalmaresController(PalmaresService palmaresService, SessionStore sessionStore) {
        this.palmaresService = palmaresService;
        this.sessionStore = sessionStore;
    }

    /**
     * Champion(s) de la semaine passée : public, comme le classement (bandeau
     * du classement, couronne, carte de partage). Avec leagueId : ceux de la ligue.
     */
    @GetMapping("/champions")
    public ResponseEntity<?> champions(@RequestParam(required = false) String leagueId) {
        return ResponseEntity.ok(palmaresService.lastWeekChampions(leagueId));
    }

    @GetMapping("/me")
    public ResponseEntity<?> mine(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(palmaresService.of(userId));
    }
}
