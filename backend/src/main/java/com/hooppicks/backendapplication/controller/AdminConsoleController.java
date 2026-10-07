package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.admin.AdminActionException;
import com.hooppicks.backendapplication.admin.AdminActionsService;
import com.hooppicks.backendapplication.admin.AdminAuditService;
import com.hooppicks.backendapplication.admin.AdminOverviewService;
import com.hooppicks.backendapplication.bet.BetResolutionService;
import com.hooppicks.backendapplication.espn.EspnPlayerStatsService;
import com.hooppicks.backendapplication.espn.EspnRosterService;
import com.hooppicks.backendapplication.espn.EspnStandingsService;
import com.hooppicks.backendapplication.dto.AdminBetDto;
import com.hooppicks.backendapplication.dto.AdminMatchDto;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Console admin de l'app : auth par cookie de session + flag User.isAdmin,
 * comme le reste de l'app : volontairement distincte de /admin/** (protégé
 * par une clé statique, cf. AdminAuthFilter) pour ne pas avoir à embarquer un
 * secret serveur côté frontend. Chaque action qui modifie quelque chose est
 * tracée dans le journal d'audit (AdminAuditService) avec l'admin exact.
 */
@RestController
@RequestMapping("/console")
public class AdminConsoleController {

    public record ReasonRequest(String reason) {}

    public record WalletAdjustRequest(Integer amount, String reason) {}

    public record AnnouncementRequest(String message, Boolean push) {}

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
    private final AdminAuditService auditService;
    private final com.hooppicks.backendapplication.season.SeasonPickService seasonPickService;
    private final AdminActionsService actionsService;
    private final AdminOverviewService overviewService;

    public AdminConsoleController(SessionStore sessionStore, UserRepository userRepository,
                                   MatchRepository matchRepository, BetRepository betRepository,
                                   NbaSyncService nbaSyncService, BetResolutionService betResolutionService,
                                   AdminSyncStatus adminSyncStatus, AccountDeletionService accountDeletionService,
                                   EspnRosterService espnRosterService, EspnStandingsService espnStandingsService,
                                   EspnPlayerStatsService espnPlayerStatsService, AdminAuditService auditService,
                                   AdminActionsService actionsService, AdminOverviewService overviewService,
                                   com.hooppicks.backendapplication.season.SeasonPickService seasonPickService) {
        this.seasonPickService = seasonPickService;
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
        this.auditService = auditService;
        this.actionsService = actionsService;
        this.overviewService = overviewService;
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();

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

    @GetMapping("/overview")
    public ResponseEntity<?> getOverview(HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();
        return ResponseEntity.ok(overviewService.overview());
    }

    public record SeasonResultRequest(String question, String team) {}

    // Bonne réponse d'une question de pronostics de saison (champion, finalistes…).
    @PostMapping("/season-results")
    public ResponseEntity<?> setSeasonResult(@RequestBody SeasonResultRequest body, HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();
        try {
            seasonPickService.setResult(body.question(), body.team());
        } catch (com.hooppicks.backendapplication.season.SeasonPickService.SeasonPickException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        auditService.log(access.admin(), "SEASON_RESULT", body.question(), body.team());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/audit")
    public ResponseEntity<?> getAudit(@RequestParam(defaultValue = "100") int limit, HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();
        return ResponseEntity.ok(auditService.recent(limit));
    }

    // --- Synchros -----------------------------------------------------

    @PostMapping("/sync-teams")
    public ResponseEntity<?> syncTeams(HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();

        int synced = nbaSyncService.syncTeams();
        auditService.log(access.admin(), "SYNC_TEAMS", "balldontlie", synced + " équipe(s)");
        return ResponseEntity.ok(Map.of("teamsSynced", synced));
    }

    @PostMapping("/sync-games")
    public ResponseEntity<?> syncGames(
            @RequestParam(defaultValue = "3") int daysAhead,
            @RequestParam(required = false) String startDate,
            HttpServletRequest request
    ) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(1);
        List<LocalDate> dates = LongStream.rangeClosed(0, daysAhead)
                .mapToObj(start::plusDays)
                .collect(Collectors.toList());
        int synced = nbaSyncService.syncGames(dates).gamesSynced();
        auditService.log(access.admin(), "SYNC_GAMES", "balldontlie",
                synced + " match(s), du " + start + " sur " + (daysAhead + 1) + " jour(s)");
        return ResponseEntity.ok(Map.of("gamesSynced", synced));
    }

    @PostMapping("/resolve-bets")
    public ResponseEntity<?> resolveBets(HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();

        int resolved = betResolutionService.resolvePendingBets();
        auditService.log(access.admin(), "RESOLVE_BETS", "paris en attente", resolved + " pari(s) résolu(s)");
        return ResponseEntity.ok(Map.of("resolved", resolved));
    }

    @PostMapping("/sync-rosters")
    public ResponseEntity<?> syncRosters(HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();

        espnRosterService.syncRosters();
        auditService.log(access.admin(), "SYNC_ROSTERS", "ESPN", null);
        return ResponseEntity.ok(Map.of("synced", true));
    }

    @PostMapping("/sync-standings")
    public ResponseEntity<?> syncStandings(HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();

        espnStandingsService.syncStandings();
        auditService.log(access.admin(), "SYNC_STANDINGS", "ESPN", null);
        return ResponseEntity.ok(Map.of("synced", true));
    }

    // Ne traite qu'un lot (cf. EspnPlayerStatsService) : ~550 joueurs au
    // total, un bouton "tout synchroniser maintenant" bloquerait la requête
    // pendant des minutes. Le rafraîchissement complet se fait en tâche de
    // fond au fil des tick du scheduler ; ce bouton sert juste à avancer
    // manuellement un lot pour tester/accélérer sans attendre.
    @PostMapping("/sync-player-stats-batch")
    public ResponseEntity<?> syncPlayerStatsBatch(HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();

        espnPlayerStatsService.syncBatch();
        auditService.log(access.admin(), "SYNC_PLAYER_STATS", "ESPN", "un lot");
        return ResponseEntity.ok(Map.of("synced", true));
    }

    // --- Utilisateurs -------------------------------------------------

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestParam(required = false) String search, HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();

        List<User> users = (search == null || search.isBlank())
                ? userRepository.findTop50ByOrderByCreatedAtDesc()
                : userRepository.findTop50ByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCreatedAtDesc(search, search);

        Map<String, Long> betCounts = new HashMap<>();
        if (!users.isEmpty()) {
            for (Object[] row : betRepository.countBetsByUser(users.stream().map(User::getId).toList())) {
                betCounts.put((String) row[0], (Long) row[1]);
            }
        }
        return ResponseEntity.ok(users.stream()
                .map(u -> AdminUserDto.from(u, betCounts.getOrDefault(u.getId(), 0L)))
                .toList());
    }

    @PostMapping("/users/{id}/toggle-admin")
    public ResponseEntity<?> toggleAdmin(@PathVariable String id, HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();

        // On ne se retire jamais soi-même le statut admin depuis ici : sinon un
        // admin seul peut se verrouiller hors de la console par erreur de clic.
        if (id.equals(access.admin().getId())) {
            return ResponseEntity.badRequest().body("Impossible de modifier ton propre statut admin ici.");
        }

        User target = userRepository.findById(id).orElse(null);
        if (target == null) return ResponseEntity.notFound().build();

        target.setAdmin(!target.isAdmin());
        userRepository.save(target);
        auditService.log(access.admin(), target.isAdmin() ? "PROMOTE_ADMIN" : "DEMOTE_ADMIN", target.getUsername(), null);
        return ResponseEntity.ok(AdminUserDto.from(target));
    }

    @PostMapping("/users/{id}/delete")
    public ResponseEntity<?> deleteUser(@PathVariable String id, HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();

        // La suppression de son propre compte passe par /auth/delete-account
        // (avec confirmation de mot de passe) : pas par cette voie admin.
        if (id.equals(access.admin().getId())) {
            return ResponseEntity.badRequest().body("Utilise la suppression de compte depuis tes paramètres.");
        }
        User target = userRepository.findById(id).orElse(null);
        if (target == null) return ResponseEntity.notFound().build();

        // Pseudo et email relevés avant suppression : le journal doit rester lisible après.
        String label = target.getUsername() + " (" + target.getEmail() + ")";
        accountDeletionService.deleteAccount(id);
        auditService.log(access.admin(), "DELETE_USER", label, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/adjust-wallet")
    public ResponseEntity<?> adjustWallet(@PathVariable String id, @RequestBody WalletAdjustRequest body,
                                          HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();
        return run(() -> Map.of("balance", actionsService.adjustWallet(id,
                body.amount() == null ? 0 : body.amount(), body.reason(), access.admin())));
    }

    // --- Matchs ---------------------------------------------------------

    @GetMapping("/matches")
    public ResponseEntity<?> getMatches(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            HttpServletRequest request
    ) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();

        MatchStatus wanted = null;
        if (status != null && !status.isBlank()) {
            try {
                wanted = MatchStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // statut inconnu dans la query string : on ignore le filtre plutôt que de 400
            }
        }
        String q = search == null || search.isBlank() ? null : "%" + search.trim().toLowerCase() + "%";
        List<Match> matches = matchRepository.searchForAdmin(wanted, q, PageRequest.of(0, 100));

        Map<String, long[]> counts = new HashMap<>();
        if (!matches.isEmpty()) {
            for (Object[] row : betRepository.countBetsByMatchAndStatus(matches.stream().map(Match::getId).toList())) {
                long[] c = counts.computeIfAbsent((String) row[0], k -> new long[2]);
                if (row[1] == BetStatus.PENDING) c[0] += (Long) row[2];
                else c[1] += (Long) row[2];
            }
        }
        return ResponseEntity.ok(matches.stream().map(m -> {
            long[] c = counts.getOrDefault(m.getId(), new long[2]);
            return new AdminMatchDto(MatchDto.from(m), Boolean.TRUE.equals(m.getAdminLocked()), c[0], c[1]);
        }).toList());
    }

    @PatchMapping("/matches/{id}")
    public ResponseEntity<?> updateMatch(
            @PathVariable String id, @RequestBody AdminUpdateMatchRequest body, HttpServletRequest request
    ) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();

        Match match = matchRepository.findById(id).orElse(null);
        if (match == null) return ResponseEntity.notFound().build();

        String before = describe(match);
        if (body.status() != null) {
            try {
                match.setStatus(MatchStatus.valueOf(body.status().toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Statut inconnu : " + body.status());
            }
        }
        if (body.homeScore() != null) match.setHomeScore(body.homeScore());
        if (body.awayScore() != null) match.setAwayScore(body.awayScore());
        // Verrouillé : la synchro suivante n'écrasera pas cette correction.
        match.setAdminLocked(true);

        matchRepository.save(match);
        auditService.log(access.admin(), "UPDATE_MATCH", matchLabel(match), before + " → " + describe(match));
        return ResponseEntity.ok(MatchDto.from(match));
    }

    @PostMapping("/matches/{id}/unlock")
    public ResponseEntity<?> unlockMatch(@PathVariable String id, HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();

        Match match = matchRepository.findById(id).orElse(null);
        if (match == null) return ResponseEntity.notFound().build();

        match.setAdminLocked(false);
        matchRepository.save(match);
        auditService.log(access.admin(), "UNLOCK_MATCH", matchLabel(match), "rendu à la synchro");
        return ResponseEntity.ok(MatchDto.from(match));
    }

    @PostMapping("/matches/{id}/void-bets")
    public ResponseEntity<?> voidMatchBets(@PathVariable String id, @RequestBody ReasonRequest body,
                                           HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();
        return run(() -> Map.of("voided", actionsService.voidPendingBetsForMatch(id, body.reason(), access.admin())));
    }

    // --- Paris -------------------------------------------------------------

    @GetMapping("/bets/pending")
    public ResponseEntity<?> getPendingBets(HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();

        return ResponseEntity.ok(betRepository.findByStatus(BetStatus.PENDING).stream()
                .map(AdminBetDto::from)
                .toList());
    }

    @PostMapping("/bets/{id}/void")
    public ResponseEntity<?> voidBet(@PathVariable String id, @RequestBody ReasonRequest body, HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();
        return run(() -> {
            actionsService.voidBet(id, body.reason(), access.admin());
            return Map.of("voided", true);
        });
    }

    // --- Annonces ------------------------------------------------------------

    @PostMapping("/announcements")
    public ResponseEntity<?> announce(@RequestBody AnnouncementRequest body, HttpServletRequest request) {
        Access access = access(request);
        if (access.denied() != null) return access.denied();
        return run(() -> Map.of("recipients",
                actionsService.announce(body.message(), Boolean.TRUE.equals(body.push()), access.admin())));
    }

    // --------------------------------------------------------------------------

    private record Access(ResponseEntity<?> denied, User admin) {}

    /**
     * 401 si pas connecté, 403 si connecté mais pas admin ; sinon l'admin
     * connecté (pour le journal d'audit). À appeler en tête de chaque méthode.
     */
    private Access access(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return new Access(ResponseEntity.status(401).build(), null);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isAdmin()) return new Access(ResponseEntity.status(403).build(), null);

        return new Access(null, user);
    }

    private static ResponseEntity<?> run(Supplier<Object> action) {
        try {
            return ResponseEntity.ok(action.get());
        } catch (AdminActionException e) {
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        }
    }

    private static String matchLabel(Match match) {
        return match.getAwayTeam().getAbbreviation() + " @ " + match.getHomeTeam().getAbbreviation();
    }

    private static String describe(Match match) {
        String score = match.getHomeScore() == null ? "-" : match.getAwayScore() + "-" + match.getHomeScore();
        return match.getStatus().name().toLowerCase() + " " + score;
    }
}
