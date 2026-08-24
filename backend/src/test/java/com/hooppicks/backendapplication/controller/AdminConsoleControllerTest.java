package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.bet.BetResolutionService;
import com.hooppicks.backendapplication.dto.AdminUpdateMatchRequest;
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

    private AdminConsoleController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminConsoleController(sessionStore, userRepository, matchRepository, betRepository,
                nbaSyncService, betResolutionService, adminSyncStatus, accountDeletionService);
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
    void getMatches_filtre_par_recherche_sur_le_nom_d_equipe() {
        HttpServletRequest request = adminRequest("admin1");
        when(matchRepository.findTop100ByOrderByDateDesc()).thenReturn(List.of(
                match("Lakers", "Celtics", MatchStatus.FINISHED),
                match("Knicks", "Nets", MatchStatus.FINISHED)
        ));

        ResponseEntity<?> response = controller.getMatches("lakers", null, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat((List<?>) response.getBody()).hasSize(1);
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
}
