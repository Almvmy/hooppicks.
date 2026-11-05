package com.hooppicks.backendapplication.social;

import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Toutes ces routes demandent un compte : ce sont des interactions entre joueurs. */
@RestController
public class SocialController {

    public record ReactRequest(String emoji) {}

    private final SocialService socialService;
    private final SessionStore sessionStore;

    public SocialController(SocialService socialService, SessionStore sessionStore) {
        this.socialService = socialService;
        this.sessionStore = sessionStore;
    }

    @GetMapping("/matches/{id}/friends")
    public ResponseEntity<?> friendsOnMatch(@PathVariable String id, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(socialService.friendsOnMatch(id, userId));
    }

    @GetMapping("/bets/reactions")
    public ResponseEntity<?> ticketReactions(@RequestParam List<String> ids, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(socialService.ticketReactions(ids, userId));
    }

    @PostMapping("/bets/{id}/react")
    public ResponseEntity<?> reactToTicket(@PathVariable String id, @RequestBody ReactRequest body, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            socialService.reactToTicket(id, userId, body.emoji());
            return ResponseEntity.ok(socialService.ticketReactions(List.of(id), userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
