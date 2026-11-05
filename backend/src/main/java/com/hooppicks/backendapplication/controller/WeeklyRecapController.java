package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.engagement.WeeklyRecapService;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeeklyRecapController {

    private final WeeklyRecapService recapService;
    private final SessionStore sessionStore;

    public WeeklyRecapController(WeeklyRecapService recapService, SessionStore sessionStore) {
        this.recapService = recapService;
        this.sessionStore = sessionStore;
    }

    /** La semaine qui vient de finir du joueur connecté (204 s'il n'a aucun ticket réglé cette semaine-là). */
    @GetMapping("/weekly-recap")
    public ResponseEntity<?> mine(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return recapService.lastWeek(userId).<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
