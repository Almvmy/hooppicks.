package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.referral.ReferralService;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/referrals")
public class ReferralController {

    private final ReferralService referralService;
    private final SessionStore sessionStore;

    public ReferralController(ReferralService referralService, SessionStore sessionStore) {
        this.referralService = referralService;
        this.sessionStore = sessionStore;
    }

    public record Mine(long recruits, int goal) {}

    @GetMapping("/me")
    public ResponseEntity<Mine> mine(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(new Mine(referralService.recruits(userId), ReferralService.RECRUITER_GOAL));
    }

    public record ClaimRequest(String ref) {}

    // Appelé par l'app juste après l'inscription, avec le pseudo du lien d'invitation.
    @PostMapping("/claim")
    public ResponseEntity<Void> claim(@RequestBody ClaimRequest body, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        referralService.claim(userId, body.ref());
        return ResponseEntity.noContent().build();
    }
}
