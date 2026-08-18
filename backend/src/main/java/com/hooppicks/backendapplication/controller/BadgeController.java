package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.badge.BadgeService;
import com.hooppicks.backendapplication.dto.BadgeDto;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/badges")
public class BadgeController {

    private final BetRepository betRepository;
    private final SessionStore sessionStore;
    private final BadgeService badgeService;

    public BadgeController(BetRepository betRepository, SessionStore sessionStore, BadgeService badgeService) {
        this.betRepository = betRepository;
        this.sessionStore = sessionStore;
        this.badgeService = badgeService;
    }

    @GetMapping
    public ResponseEntity<List<BadgeDto>> getBadges(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        List<Bet> bets = betRepository.findByUserIdOrderByPlacedAtDesc(userId);
        return ResponseEntity.ok(badgeService.computeBadges(bets));
    }
}