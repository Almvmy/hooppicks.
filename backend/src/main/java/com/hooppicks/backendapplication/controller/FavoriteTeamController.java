package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.favorite.FavoriteTeamService;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/favorite-team")
public class FavoriteTeamController {

    private final SessionStore sessionStore;
    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final FavoriteTeamService favoriteTeamService;

    public FavoriteTeamController(SessionStore sessionStore, UserRepository userRepository,
                                  BetRepository betRepository, FavoriteTeamService favoriteTeamService) {
        this.sessionStore = sessionStore;
        this.userRepository = userRepository;
        this.betRepository = betRepository;
        this.favoriteTeamService = favoriteTeamService;
    }

    /** Tes paris vus depuis ton équipe favorite (teamAbbreviation null si tu n'en as pas). */
    @GetMapping("/stats")
    public ResponseEntity<FavoriteTeamService.Stats> stats(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(favoriteTeamService.stats(user.getFavoriteTeam(),
                betRepository.findByUserIdOrderByPlacedAtDesc(userId)));
    }
}
