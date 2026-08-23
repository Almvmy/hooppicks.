package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.badge.BadgeService;
import com.hooppicks.backendapplication.dto.PublicProfileDto;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Profil consultable par n'importe quel utilisateur connecté (classement,
 * membres de ligue, fil d'activité) — jamais anonyme, contrairement à
 * /matches ou /leaderboard. Ne renvoie que le sous-ensemble public de User,
 * voir PublicProfileDto.
 */
@RestController
@RequestMapping("/users")
public class PublicProfileController {

    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final BadgeService badgeService;
    private final SessionStore sessionStore;

    public PublicProfileController(UserRepository userRepository, BetRepository betRepository,
                                    BadgeService badgeService, SessionStore sessionStore) {
        this.userRepository = userRepository;
        this.betRepository = betRepository;
        this.badgeService = badgeService;
        this.sessionStore = sessionStore;
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> getPublicProfile(@PathVariable String username, HttpServletRequest request) {
        if (sessionStore.getUserIdFromRequest(request) == null) return ResponseEntity.status(401).build();

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        List<Bet> bets = betRepository.findByUserIdOrderByPlacedAtDesc(user.getId());
        List<Object[]> stats = betRepository.getUserStats(user.getId());
        long totalBets = 0;
        long wonBets = 0;
        if (!stats.isEmpty() && stats.get(0)[0] != null) {
            totalBets = (Long) stats.get(0)[0];
            wonBets = stats.get(0)[1] != null ? (Long) stats.get(0)[1] : 0;
        }
        int winRate = totalBets == 0 ? 0 : (int) Math.round((wonBets * 100.0) / totalBets);

        return ResponseEntity.ok(PublicProfileDto.from(user, winRate, (int) totalBets, badgeService.computeBadges(bets)));
    }
}
