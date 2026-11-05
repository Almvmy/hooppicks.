package com.hooppicks.backendapplication.bracket;

import com.hooppicks.backendapplication.admin.AdminAuditService;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
public class BracketController {

    public record PicksRequest(List<String> picks) {}
    public record PublishRequest(String id, String label, List<String> teams, String locksAt) {}
    public record WinnerRequest(int game, String team) {}

    private final BracketService service;
    private final SessionStore sessionStore;
    private final UserRepository userRepository;
    private final AdminAuditService auditService;

    public BracketController(BracketService service, SessionStore sessionStore, UserRepository userRepository, AdminAuditService auditService) {
        this.service = service;
        this.sessionStore = sessionStore;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /** 204 : pas encore de tableau (avant la fin des poules). */
    @GetMapping("/bracket")
    public ResponseEntity<?> current(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return service.current(userId).<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/bracket/picks")
    public ResponseEntity<?> picks(@RequestBody PicksRequest body, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            return ResponseEntity.ok(service.savePicks(userId, body.picks()));
        } catch (BracketService.BracketException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private User admin(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        return userId == null ? null : userRepository.findById(userId).filter(User::isAdmin).orElse(null);
    }

    @PostMapping("/console/bracket")
    public ResponseEntity<?> publish(@RequestBody PublishRequest body, HttpServletRequest request) {
        User admin = admin(request);
        if (admin == null) return ResponseEntity.status(403).build();
        try {
            service.publish(body.id(), body.label(), body.teams(), Instant.parse(body.locksAt()));
        } catch (BracketService.BracketException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (DateTimeParseException | NullPointerException e) {
            return ResponseEntity.badRequest().body("Heure du premier match invalide.");
        }
        auditService.log(admin, "Tableau publié", body.id(), String.join(",", body.teams()) + " · " + body.locksAt());
        return ResponseEntity.ok(Map.of("published", true));
    }

    @PostMapping("/console/bracket/winner")
    public ResponseEntity<?> winner(@RequestBody WinnerRequest body, HttpServletRequest request) {
        User admin = admin(request);
        if (admin == null) return ResponseEntity.status(403).build();
        try {
            service.setWinner(body.game(), body.team());
        } catch (BracketService.BracketException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        auditService.log(admin, "Vainqueur du tableau saisi", "match " + body.game(), body.team());
        return ResponseEntity.ok(Map.of("saved", true));
    }
}
