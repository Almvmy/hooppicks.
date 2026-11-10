package com.hooppicks.backendapplication.bet;

import com.hooppicks.backendapplication.dto.PlacedBetDto;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class CashOutController {

    public record CashOutRequest(int expected) {}

    private final CashOutService cashOutService;
    private final SessionStore sessionStore;

    public CashOutController(CashOutService cashOutService, SessionStore sessionStore) {
        this.cashOutService = cashOutService;
        this.sessionStore = sessionStore;
    }

    @GetMapping("/bets/cash-out")
    public ResponseEntity<?> quotes(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(cashOutService.quotes(userId));
    }

    @PostMapping("/bets/{id}/cash-out")
    public ResponseEntity<?> cashOut(@PathVariable String id, @RequestBody CashOutRequest body, HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            return ResponseEntity.ok(PlacedBetDto.from(cashOutService.cashOut(userId, id, body == null ? 0 : body.expected())));
        } catch (CashOutService.CashOutException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            if (e.value() != null) error.put("value", e.value());
            return ResponseEntity.status(e.status()).body(error);
        }
    }
}
