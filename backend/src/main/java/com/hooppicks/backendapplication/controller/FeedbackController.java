package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.admin.AdminActionException;
import com.hooppicks.backendapplication.feedback.FeedbackService;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** « Nous écrire » : un joueur envoie un message à l'équipe et retrouve les réponses. */
@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final SessionStore sessionStore;

    public FeedbackController(FeedbackService feedbackService, SessionStore sessionStore) {
        this.feedbackService = feedbackService;
        this.sessionStore = sessionStore;
    }

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody FeedbackService.SubmitRequest body, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            return ResponseEntity.ok(feedbackService.submit(userId, body, request.getHeader("User-Agent")));
        } catch (AdminActionException e) {
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<?> mine(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(feedbackService.mine(userId));
    }
}
