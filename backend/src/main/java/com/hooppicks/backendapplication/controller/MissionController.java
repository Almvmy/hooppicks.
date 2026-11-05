package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.engagement.MissionService;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MissionController {

    private final MissionService missionService;
    private final SessionStore sessionStore;

    public MissionController(MissionService missionService, SessionStore sessionStore) {
        this.missionService = missionService;
        this.sessionStore = sessionStore;
    }

    @GetMapping("/missions")
    public ResponseEntity<?> mine(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(missionService.forUser(userId));
    }
}
