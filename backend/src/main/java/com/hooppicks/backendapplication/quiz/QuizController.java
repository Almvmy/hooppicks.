package com.hooppicks.backendapplication.quiz;

import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.function.Function;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    public record AnswerRequest(int index, Integer choice) {}

    private final QuizService service;
    private final SessionStore sessionStore;

    public QuizController(QuizService service, SessionStore sessionStore) {
        this.service = service;
        this.sessionStore = sessionStore;
    }

    private ResponseEntity<?> run(HttpServletRequest request, Function<String, Object> action) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            return ResponseEntity.ok(action.apply(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> state(HttpServletRequest request) {
        return run(request, service::state);
    }

    @PostMapping("/start")
    public ResponseEntity<?> start(HttpServletRequest request) {
        return run(request, service::start);
    }

    @PostMapping("/answer")
    public ResponseEntity<?> answer(@RequestBody AnswerRequest body, HttpServletRequest request) {
        return run(request, userId -> service.answer(userId, body.index(), body.choice()));
    }

    @GetMapping("/board")
    public ResponseEntity<?> board(HttpServletRequest request) {
        return run(request, service::board);
    }
}
