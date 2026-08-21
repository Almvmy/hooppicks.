package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.bet.BetResolutionService;
import com.hooppicks.backendapplication.dto.AdminStatusDto;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.nba.AdminSyncStatus;
import com.hooppicks.backendapplication.nba.NbaSyncService;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Console admin de l'app : auth par cookie de session + flag User.isAdmin,
 * comme le reste de l'app — volontairement distincte de /admin/** (protégé
 * par une clé statique, cf. AdminAuthFilter) pour ne pas avoir à embarquer un
 * secret serveur côté frontend.
 */
@RestController
@RequestMapping("/console")
public class AdminConsoleController {

    private final SessionStore sessionStore;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final BetRepository betRepository;
    private final NbaSyncService nbaSyncService;
    private final BetResolutionService betResolutionService;
    private final AdminSyncStatus adminSyncStatus;

    public AdminConsoleController(SessionStore sessionStore, UserRepository userRepository,
                                   MatchRepository matchRepository, BetRepository betRepository,
                                   NbaSyncService nbaSyncService, BetResolutionService betResolutionService,
                                   AdminSyncStatus adminSyncStatus) {
        this.sessionStore = sessionStore;
        this.userRepository = userRepository;
        this.matchRepository = matchRepository;
        this.betRepository = betRepository;
        this.nbaSyncService = nbaSyncService;
        this.betResolutionService = betResolutionService;
        this.adminSyncStatus = adminSyncStatus;
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(HttpServletRequest request) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) return denied;

        AdminStatusDto status = new AdminStatusDto(
                adminSyncStatus.getLastSyncAt(),
                adminSyncStatus.getLastGamesSynced(),
                adminSyncStatus.getLastBetsResolved(),
                adminSyncStatus.getMode(),
                userRepository.count(),
                matchRepository.count(),
                betRepository.findByStatus(BetStatus.PENDING).size()
        );
        return ResponseEntity.ok(status);
    }

    @PostMapping("/sync-teams")
    public ResponseEntity<?> syncTeams(HttpServletRequest request) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) return denied;

        return ResponseEntity.ok(Map.of("teamsSynced", nbaSyncService.syncTeams()));
    }

    @PostMapping("/sync-games")
    public ResponseEntity<?> syncGames(
            @RequestParam(defaultValue = "3") int daysAhead,
            @RequestParam(required = false) String startDate,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) return denied;

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(1);
        List<LocalDate> dates = LongStream.rangeClosed(0, daysAhead)
                .mapToObj(start::plusDays)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("gamesSynced", nbaSyncService.syncGames(dates).gamesSynced()));
    }

    @PostMapping("/resolve-bets")
    public ResponseEntity<?> resolveBets(HttpServletRequest request) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) return denied;

        return ResponseEntity.ok(Map.of("resolved", betResolutionService.resolvePendingBets()));
    }

    /**
     * 401 si pas connecté, 403 si connecté mais pas admin, null si tout va bien —
     * à appeler en tête de chaque méthode plutôt que de dupliquer les deux checks.
     */
    private ResponseEntity<?> requireAdmin(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isAdmin()) return ResponseEntity.status(403).build();

        return null;
    }
}
