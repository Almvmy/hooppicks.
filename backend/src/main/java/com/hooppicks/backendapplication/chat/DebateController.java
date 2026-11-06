package com.hooppicks.backendapplication.chat;

import com.hooppicks.backendapplication.admin.AdminAuditService;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.DebateRepository;
import com.hooppicks.backendapplication.repository.DebateVoteRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Function;

@RestController
public class DebateController {

    public record VoteRequest(String side) {}
    public record CommentRequest(String text, String replyToId) {}
    public record PlanRequest(String day, String question, String optionA, String optionB) {}
    public record SharedDebate(String day, String question, String optionA, String optionB, long votesA, long votesB) {}

    private final DebateService service;
    private final DebateRepository debateRepository;
    private final DebateVoteRepository voteRepository;
    private final UserRepository userRepository;
    private final SessionStore sessionStore;
    private final AdminAuditService auditService;

    public DebateController(DebateService service, DebateRepository debateRepository, DebateVoteRepository voteRepository,
                            UserRepository userRepository, SessionStore sessionStore, AdminAuditService auditService) {
        this.service = service;
        this.debateRepository = debateRepository;
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
        this.sessionStore = sessionStore;
        this.auditService = auditService;
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

    private static LocalDate parse(String day) {
        try {
            return LocalDate.parse(day);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date invalide.");
        }
    }

    @GetMapping("/debates/today")
    public ResponseEntity<?> today(HttpServletRequest request) {
        return run(request, service::today);
    }

    @PostMapping("/debates/today/vote")
    public ResponseEntity<?> vote(@RequestBody VoteRequest body, HttpServletRequest request) {
        return run(request, userId -> service.vote(userId, body.side()));
    }

    @GetMapping("/debates/{day}")
    public ResponseEntity<?> ofDay(@PathVariable String day, HttpServletRequest request) {
        return run(request, userId -> service.ofDay(userId, parse(day)).orElseThrow(() -> new IllegalArgumentException("Débat introuvable.")));
    }

    @GetMapping("/debates/{day}/comments")
    public ResponseEntity<?> comments(@PathVariable String day, HttpServletRequest request) {
        return run(request, userId -> service.comments(userId, parse(day)));
    }

    @PostMapping("/debates/{day}/comments")
    public ResponseEntity<?> comment(@PathVariable String day, @RequestBody CommentRequest body, HttpServletRequest request) {
        return run(request, userId -> service.comment(userId, parse(day), body.text(), body.replyToId()));
    }

    /** Pour la carte à partager : la question et les totaux, rien sur les joueurs. */
    @GetMapping("/share/debates/{day}")
    public ResponseEntity<?> shared(@PathVariable String day) {
        LocalDate date;
        try {
            date = parse(day);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return debateRepository.findById(date).filter(d -> voteRepository.countByDay(date) > 0)
                .<ResponseEntity<?>>map(d -> ResponseEntity.ok(new SharedDebate(day, d.getQuestion(), d.getOptionA(), d.getOptionB(),
                        voteRepository.countByDayAndSide(date, "A"), voteRepository.countByDayAndSide(date, "B"))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- Console ---

    private User admin(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        return userId == null ? null : userRepository.findById(userId).filter(User::isAdmin).orElse(null);
    }

    @GetMapping("/console/debates")
    public ResponseEntity<?> planning(HttpServletRequest request) {
        if (admin(request) == null) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(service.planning());
    }

    @PostMapping("/console/debates")
    public ResponseEntity<?> plan(@RequestBody PlanRequest body, HttpServletRequest request) {
        User admin = admin(request);
        if (admin == null) return ResponseEntity.status(403).build();
        try {
            service.plan(parse(body.day()), body.question(), body.optionA(), body.optionB());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        auditService.log(admin, "Débat prévu", body.day(), body.question());
        return ResponseEntity.ok(service.planning());
    }

    @PostMapping("/console/debates/{day}/reset")
    public ResponseEntity<?> unplan(@PathVariable String day, HttpServletRequest request) {
        User admin = admin(request);
        if (admin == null) return ResponseEntity.status(403).build();
        try {
            service.unplan(parse(day));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        auditService.log(admin, "Débat prévu retiré", day, null);
        return ResponseEntity.ok(service.planning());
    }
}
