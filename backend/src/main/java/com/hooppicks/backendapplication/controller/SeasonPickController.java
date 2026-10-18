package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.season.SeasonPickService;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/season-picks")
public class SeasonPickController {

    private final SeasonPickService service;
    private final SessionStore sessionStore;

    public SeasonPickController(SeasonPickService service, SessionStore sessionStore) {
        this.service = service;
        this.sessionStore = sessionStore;
    }

    // Public : un lien partagé montre la page à qui n'a pas de compte (sans pronostics à lui).
    @GetMapping
    public ResponseEntity<?> overview(HttpServletRequest request) {
        return ResponseEntity.ok(service.overview(sessionStore.getUserIdFromRequest(request)));
    }

    public record PickRequest(String question, String team) {}

    @PutMapping
    public ResponseEntity<?> pick(@RequestBody PickRequest body, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            service.pick(userId, body.question(), body.team());
            return ResponseEntity.ok().build();
        } catch (SeasonPickService.SeasonPickException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
