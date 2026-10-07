package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.duel.DuelService;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/duels")
public class DuelController {

    private final DuelService duelService;
    private final SessionStore sessionStore;

    public DuelController(DuelService duelService, SessionStore sessionStore) {
        this.duelService = duelService;
        this.sessionStore = sessionStore;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(duelService.list(userId));
    }

    public record ChallengeRequest(String opponent) {}

    @PostMapping
    public ResponseEntity<?> challenge(@RequestBody ChallengeRequest body, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return run(() -> Map.of("id", duelService.challenge(userId, body.opponent()).getId()));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable String id, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return run(() -> Map.of("status", duelService.respond(userId, id, true).getStatus().name().toLowerCase()));
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<?> decline(@PathVariable String id, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return run(() -> Map.of("status", duelService.respond(userId, id, false).getStatus().name().toLowerCase()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String id, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return run(() -> Map.of("status", duelService.cancel(userId, id).getStatus().name().toLowerCase()));
    }

    private ResponseEntity<?> run(Supplier<Object> action) {
        try {
            return ResponseEntity.ok(action.get());
        } catch (DuelService.DuelException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
