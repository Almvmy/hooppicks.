package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.bet.BetResolutionService;
import com.hooppicks.backendapplication.espn.EspnPlayerStatsService;
import com.hooppicks.backendapplication.espn.EspnRosterService;
import com.hooppicks.backendapplication.espn.EspnStandingsService;
import com.hooppicks.backendapplication.dto.AdminBetDto;
import com.hooppicks.backendapplication.dto.AdminStatusDto;
import com.hooppicks.backendapplication.dto.AdminUpdateMatchRequest;
import com.hooppicks.backendapplication.dto.AdminUserDto;
import com.hooppicks.backendapplication.dto.MatchDto;
import com.hooppicks.backendapplication.entity.BetStatus;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.nba.AdminSyncStatus;
import com.hooppicks.backendapplication.nba.NbaSyncService;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.security.AccountDeletionService;
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
    private final AccountDeletionService accountDeletionService;
    private final EspnRosterService espnRosterService;
    private final EspnStandingsService espnStandingsService;
    private final EspnPlayerStatsService espnPlayerStatsService;

    public AdminConsoleController(SessionStore sessionStore, UserRepository userRepository,
                                   MatchRepository matchRepository, BetRepository betRepository,
                                   NbaSyncService nbaSyncService, BetResolutionService betResolutionService,
                                   AdminSyncStatus adminSyncStatus, AccountDeletionService accountDeletionService,
                                   EspnRosterService espnRosterService, EspnStandingsService espnStandingsService,
                                   EspnPlayerStatsService espnPlayerStatsService) {
        this.sessionStore = sessionStore;
        this.userRepository = userRepository;
        this.matchRepository = matchRepository;
        this.betRepository = betRepository;
        this.nbaSyncService = nbaSyncService;
        this.betResolutionService = betResolutionService;
        this.adminSyncStatus = adminSyncStatus;
        this.accountDeletionService = accountDeletionService;
        this.espnRosterService = espnRosterService;
        this.espnStandingsService = espnStandingsService;
        this.espnPlayerStatsService = espnPlayerStatsService;
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

    @PostMapping("/sync-rosters")
    public ResponseEntity<?> syncRosters(HttpServletRequest request) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) return denied;

        espnRosterService.syncRosters();
        return ResponseEntity.ok(Map.of("synced", true));
    }

    @PostMapping("/sync-standings")
    public ResponseEntity<?> syncStandings(HttpServletRequest request) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) return denied;

        espnStandingsService.syncStandings();
        return ResponseEntity.ok(Map.of("synced", true));
    }

    // Ne traite qu'un lot (cf. EspnPlayerStatsService) — ~550 joueurs au
    // total, un bouton "tout synchroniser maintenant" bloquerait la requête
    // pendant des minutes. Le rafraîchissement complet se fait en tâche de
    // fond au fil des tick du scheduler ; ce bouton sert juste à avancer
    // manuellement un lot pour tester/accélérer sans attendre.
    @PostMapping("/sync-player-stats-batch")
    public ResponseEntity<?> syncPlayerStatsBatch(HttpServletRequest request) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) return denied;

        espnPlayerStatsService.syncBatch();
        return ResponseEntity.ok(Map.of("synced", true));
    }

    // --- Utilisateurs -------------------------------------------------

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestParam(required = false) String search, HttpServletRequest request) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) return denied;

        List<User> users = (search == null || search.isBlank())
                ? userRepository.findTop50ByOrderByCreatedAtDesc()
                : userRepository.findTop50ByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCreatedAtDesc(search, search);

        return ResponseEntity.ok(users.stream().map(AdminUserDto::from).toList());
    }

    @PostMapping("/users/{id}/toggle-admin")
    public ResponseEntity<?> toggleAdmin(@PathVariable String id, HttpServletRequest request) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) return denied;

        // On ne se retire jamais soi-même le statut admin depuis ici — sinon un
        // admin seul peut se verrouiller hors de la console par erreur de clic.
        if (id.equals(sessionStore.getUserIdFromRequest(request))) {
            return ResponseEntity.badRequest().body("Impossible de modifier ton propre statut admin ici.");
        }

        User target = userRepository.findById(id).orElse(null);
        if (target == null) return ResponseEntity.notFound().build();

        target.setAdmin(!target.isAdmin());
        userRepository.save(target);
        return ResponseEntity.ok(AdminUserDto.from(target));
    }

    @PostMapping("/users/{id}/delete")
    public ResponseEntity<?> deleteUser(@PathVariable String id, HttpServletRequest request) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) return denied;

        // La suppression de son propre compte passe par /auth/delete-account
        // (avec confirmation de mot de passe) — pas par cette voie admin.
        if (id.equals(sessionStore.getUserIdFromRequest(request))) {
            return ResponseEntity.badRequest().body("Utilise la suppression de compte depuis tes paramètres.");
        }
        if (userRepository.findById(id).isEmpty()) return ResponseEntity.notFound().build();

        accountDeletionService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    // --- Matchs ---------------------------------------------------------

    @GetMapping("/matches")
    public ResponseEntity<?> getMatches(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            HttpServletRequest request
    ) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) return denied;

        List<Match> matches = matchRepository.findTop100ByOrderByDateDesc();

        if (status != null && !status.isBlank()) {
            try {
                MatchStatus wanted = MatchStatus.valueOf(status.toUpperCase());
                matches = matches.stream().filter(m -> m.getStatus() == wanted).toList();
            } catch (IllegalArgumentException ignored) {
                // statut inconnu dans la query string : on ignore le filtre plutôt que de 400
            }
        }
        if (search != null && !search.isBlank()) {
            String needle = search.toLowerCase();
            matches = matches.stream()
                    .filter(m -> m.getHomeTeam().getName().toLowerCase().contains(needle)
                            || m.getAwayTeam().getName().toLowerCase().contains(needle))
                    .toList();
        }

        return ResponseEntity.ok(matches.stream().map(MatchDto::from).toList());
    }

    @PatchMapping("/matches/{id}")
    public ResponseEntity<?> updateMatch(
            @PathVariable String id, @RequestBody AdminUpdateMatchRequest body, HttpServletRequest request
    ) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) return denied;

        Match match = matchRepository.findById(id).orElse(null);
        if (match == null) return ResponseEntity.notFound().build();

        if (body.status() != null) {
            try {
                match.setStatus(MatchStatus.valueOf(body.status().toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Statut inconnu : " + body.status());
            }
        }
        if (body.homeScore() != null) match.setHomeScore(body.homeScore());
        if (body.awayScore() != null) match.setAwayScore(body.awayScore());

        matchRepository.save(match);
        return ResponseEntity.ok(MatchDto.from(match));
    }

    // --- Paris en attente -------------------------------------------------

    @GetMapping("/bets/pending")
    public ResponseEntity<?> getPendingBets(HttpServletRequest request) {
        ResponseEntity<?> denied = requireAdmin(request);
        if (denied != null) return denied;

        return ResponseEntity.ok(betRepository.findByStatus(BetStatus.PENDING).stream()
                .map(AdminBetDto::from)
                .toList());
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
