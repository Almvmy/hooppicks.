package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.live.MatchFollowService;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class MatchFollowController {

    private final MatchFollowService followService;
    private final SessionStore sessionStore;

    public MatchFollowController(MatchFollowService followService, SessionStore sessionStore) {
        this.followService = followService;
        this.sessionStore = sessionStore;
    }

    /** Ids des matchs que je suis. */
    @GetMapping("/follows")
    public ResponseEntity<?> mine(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(followService.followedMatchIds(userId));
    }

    @PostMapping("/matches/{id}/follow")
    public ResponseEntity<?> follow(@PathVariable String id, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            followService.follow(userId, id);
            return ResponseEntity.ok().build();
        } catch (MatchFollowService.FollowException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/matches/{id}/follow")
    public ResponseEntity<?> unfollow(@PathVariable String id, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        followService.unfollow(userId, id);
        return ResponseEntity.ok().build();
    }
}
