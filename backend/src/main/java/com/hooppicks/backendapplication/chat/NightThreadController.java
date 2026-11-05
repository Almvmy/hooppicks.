package com.hooppicks.backendapplication.chat;

import com.hooppicks.backendapplication.league.LeagueService;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.function.Function;
import java.util.function.Supplier;

/** Le fil se lit et s'écrit avec un compte : c'est aussi ce qui permet de le modérer. */
@RestController
@RequestMapping("/night-thread")
public class NightThreadController {

    public record PostRequest(String text, String matchId) {}
    public record ReactRequest(String emoji) {}
    public record UsernameRequest(String username) {}

    private final NightThreadService service;
    private final LeagueService leagueService;
    private final SessionStore sessionStore;

    public NightThreadController(NightThreadService service, LeagueService leagueService, SessionStore sessionStore) {
        this.service = service;
        this.leagueService = leagueService;
        this.sessionStore = sessionStore;
    }

    private ResponseEntity<?> run(HttpServletRequest request, Function<String, Object> action) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            return ResponseEntity.ok(action.apply(userId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> feed(@RequestParam(required = false) String matchId, HttpServletRequest request) {
        return run(request, userId -> service.feed(userId, matchId));
    }

    @PostMapping
    public ResponseEntity<?> post(@RequestBody PostRequest body, HttpServletRequest request) {
        return run(request, userId -> {
            service.post(userId, body.text(), body.matchId());
            return service.feed(userId, body.matchId());
        });
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<?> delete(@PathVariable String id, HttpServletRequest request) {
        return run(request, userId -> {
            service.delete(userId, id);
            return java.util.Map.of("deleted", true);
        });
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<?> report(@PathVariable String id, HttpServletRequest request) {
        return run(request, userId -> {
            service.report(userId, id);
            return java.util.Map.of("reported", true);
        });
    }

    @PostMapping("/{id}/react")
    public ResponseEntity<?> react(@PathVariable String id, @RequestBody ReactRequest body, HttpServletRequest request) {
        return run(request, userId -> {
            leagueService.applyReaction("NIGHT", id, userId, body.emoji());
            return java.util.Map.of("ok", true);
        });
    }

    @GetMapping("/blocks")
    public ResponseEntity<?> blocks(HttpServletRequest request) {
        return run(request, service::blocks);
    }

    @PostMapping("/block")
    public ResponseEntity<?> block(@RequestBody UsernameRequest body, HttpServletRequest request) {
        return run(request, userId -> {
            service.block(userId, body.username());
            return service.blocks(userId);
        });
    }

    @PostMapping("/unblock")
    public ResponseEntity<?> unblock(@RequestBody UsernameRequest body, HttpServletRequest request) {
        return run(request, userId -> {
            service.unblock(userId, body.username());
            return service.blocks(userId);
        });
    }
}
