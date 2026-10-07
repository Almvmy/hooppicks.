package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.admin.AdminActionsService;
import com.hooppicks.backendapplication.admin.AdminAuditService;
import com.hooppicks.backendapplication.admin.AdminOverviewService;
import com.hooppicks.backendapplication.bet.BetResolutionService;
import com.hooppicks.backendapplication.dto.AdminUpdateMatchRequest;
import com.hooppicks.backendapplication.espn.EspnPlayerStatsService;
import com.hooppicks.backendapplication.espn.EspnRosterService;
import com.hooppicks.backendapplication.espn.EspnStandingsService;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.nba.AdminSyncStatus;
import com.hooppicks.backendapplication.nba.NbaSyncService;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.security.AccountDeletionService;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminConsoleControllerTest {

    @Mock
    private SessionStore sessionStore;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private BetRepository betRepository;
    @Mock
    private NbaSyncService nbaSyncService;
    @Mock
    private BetResolutionService betResolutionService;
    @Mock
    private AdminSyncStatus adminSyncStatus;
    @Mock
    private AccountDeletionService accountDeletionService;
    @Mock
    private EspnRosterService espnRosterService;
    @Mock
    private EspnStandingsService espnStandingsService;
    @Mock
    private EspnPlayerStatsService espnPlayerStatsService;
    @Mock
    private AdminAuditService auditService;
    @Mock
    private AdminActionsService actionsService;
    @Mock
    private AdminOverviewService overviewService;

    private AdminConsoleController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminConsoleController(sessionStore, userRepository, matchRepository, betRepository,
                nbaSyncService, betResolutionService, adminSyncStatus, accountDeletionService, espnRosterService,
                espnStandingsService, espnPlayerStatsService, auditService, actionsService, overviewService,
                mock(com.hooppicks.backendapplication.season.SeasonPickService.class));
    }

    private HttpServletRequest adminRequest(String adminId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("hp_session", "token-" + adminId));
        when(sessionStore.getUserIdFromRequest(request)).thenReturn(adminId);
        User admin = new User();
        admin.setId(adminId);
        admin.setAdmin(true);
        lenient().when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        return request;
    }

    @Test
    void getUsers_sans_session_renvoie_401() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(sessionStore.getUserIdFromRequest(request)).thenReturn(null);

        ResponseEntity<?> response = controller.getUsers(null, request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void getUsers_pour_un_non_admin_renvoie_403() {
        HttpServletRequest request = new MockHttpServletRequest();
        when(sessionStore.getUserIdFromRequest(request)).thenReturn("u1");
        User nonAdmin = new User();
        nonAdmin.setId("u1");
        nonAdmin.setAdmin(false);
        when(userRepository.findById("u1")).thenReturn(Optional.of(nonAdmin));

        ResponseEntity<?> response = controller.getUsers(null, request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void toggleAdmin_sur_soi_meme_est_refuse() {
        HttpServletRequest request = adminRequest("admin1");

        ResponseEntity<?> response = controller.toggleAdmin("admin1", request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(userRepository, never()).save(any());
    }

    @Test
    void toggleAdmin_sur_un_autre_utilisateur_bascule_le_statut() {
        HttpServletRequest request = adminRequest("admin1");
        User target = new User();
        target.setId("u2");
        target.setAdmin(false);
        when(userRepository.findById("u2")).thenReturn(Optional.of(target));

        ResponseEntity<?> response = controller.toggleAdmin("u2", request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(target.isAdmin()).isTrue();
        verify(userRepository).save(target);
    }

    @Test
    void deleteUser_sur_soi_meme_est_refuse() {
        HttpServletRequest request = adminRequest("admin1");

        ResponseEntity<?> response = controller.deleteUser("admin1", request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(accountDeletionService);
    }

    @Test
    void deleteUser_sur_un_autre_utilisateur_supprime_le_compte() {
        HttpServletRequest request = adminRequest("admin1");
        when(userRepository.findById("u2")).thenReturn(Optional.of(new User()));

        ResponseEntity<?> response = controller.deleteUser("u2", request);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(accountDeletionService).deleteAccount("u2");
    }

    @Test
    void deleteUser_introuvable_renvoie_404() {
        HttpServletRequest request = adminRequest("admin1");
        when(userRepository.findById("u2")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.deleteUser("u2", request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        verifyNoInteractions(accountDeletionService);
    }

    private Match match(String homeTeamName, String awayTeamName, MatchStatus status) {
        Match m = new Match();
        m.setId("m1");
        m.setStatus(status);
        m.setDate(java.time.Instant.now());
        Team home = new Team();
        home.setName(homeTeamName);
        Team away = new Team();
        away.setName(awayTeamName);
        m.setHomeTeam(home);
        m.setAwayTeam(away);
        return m;
    }

    @Test
    void getMatches_filtre_en_base_par_statut_et_recherche() {
        HttpServletRequest request = adminRequest("admin1");
        when(matchRepository.searchForAdmin(eq(MatchStatus.FINISHED), eq("%lakers%"), any()))
                .thenReturn(List.of(match("Lakers", "Celtics", MatchStatus.FINISHED)));

        ResponseEntity<?> response = controller.getMatches(" Lakers ", "finished", request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat((List<?>) response.getBody()).hasSize(1);
    }

    @Test
    void getMatches_sans_filtre_ni_statut_connu() {
        HttpServletRequest request = adminRequest("admin1");
        when(matchRepository.searchForAdmin(isNull(), isNull(), any())).thenReturn(List.of());

        ResponseEntity<?> response = controller.getMatches(null, "pas_un_statut", request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void updateMatch_corrige_le_score_et_le_statut() {
        HttpServletRequest request = adminRequest("admin1");
        Match match = match("Lakers", "Celtics", MatchStatus.LIVE);
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));

        AdminUpdateMatchRequest body = new AdminUpdateMatchRequest("finished", 110, 102);
        ResponseEntity<?> response = controller.updateMatch("m1", body, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(match.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(match.getHomeScore()).isEqualTo(110);
        assertThat(match.getAwayScore()).isEqualTo(102);
        verify(matchRepository).save(match);
    }

    @Test
    void updateMatch_avec_un_statut_inconnu_renvoie_400() {
        HttpServletRequest request = adminRequest("admin1");
        Match match = match("Lakers", "Celtics", MatchStatus.LIVE);
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));

        AdminUpdateMatchRequest body = new AdminUpdateMatchRequest("not_a_status", null, null);
        ResponseEntity<?> response = controller.updateMatch("m1", body, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(matchRepository, never()).save(any());
    }

    @Test
    void updateMatch_verrouille_le_match_et_trace_l_action() {
        HttpServletRequest request = adminRequest("admin1");
        Match match = match("Lakers", "Celtics", MatchStatus.LIVE);
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));

        controller.updateMatch("m1", new AdminUpdateMatchRequest("finished", 110, 102), request);

        // Verrouillé : la synchro suivante ne doit pas écraser la correction.
        assertThat(match.getAdminLocked()).isTrue();
        verify(auditService).log(any(User.class), eq("UPDATE_MATCH"), anyString(), contains("finished 102-110"));
    }

    @Test
    void unlockMatch_rend_le_match_a_la_synchro() {
        HttpServletRequest request = adminRequest("admin1");
        Match match = match("Lakers", "Celtics", MatchStatus.FINISHED);
        match.setAdminLocked(true);
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));

        controller.unlockMatch("m1", request);

        assertThat(match.getAdminLocked()).isFalse();
        verify(auditService).log(any(User.class), eq("UNLOCK_MATCH"), anyString(), anyString());
    }

    @Test
    void action_refusee_par_le_service_renvoie_son_statut_et_son_message() {
        HttpServletRequest request = adminRequest("admin1");
        when(actionsService.adjustWallet(eq("u2"), eq(-5000), anyString(), any(User.class)))
                .thenThrow(new com.hooppicks.backendapplication.admin.AdminActionException(400, "Le solde deviendrait négatif."));

        ResponseEntity<?> response = controller.adjustWallet("u2",
                new AdminConsoleController.WalletAdjustRequest(-5000, "Correction"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Le solde deviendrait négatif.");
    }

    @Test
    void nouvelles_routes_refusees_aux_non_admins() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(sessionStore.getUserIdFromRequest(request)).thenReturn("u1");
        User user = new User();
        user.setId("u1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        assertThat(controller.voidBet("b1", new AdminConsoleController.ReasonRequest("x"), request).getStatusCode().value()).isEqualTo(403);
        assertThat(controller.announce(new AdminConsoleController.AnnouncementRequest("x", false), request).getStatusCode().value()).isEqualTo(403);
        assertThat(controller.getAudit(10, request).getStatusCode().value()).isEqualTo(403);
        verifyNoInteractions(actionsService, auditService);
    }
}
