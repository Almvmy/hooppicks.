package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.CreateLeagueRequest;
import com.hooppicks.backendapplication.dto.JoinLeagueRequest;
import com.hooppicks.backendapplication.dto.LeagueDto;
import com.hooppicks.backendapplication.league.LeagueService;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeagueControllerTest {

    @Mock
    private LeagueService leagueService;
    @Mock
    private SessionStore sessionStore;

    private LeagueController controller;

    @BeforeEach
    void setUp() {
        controller = new LeagueController(leagueService, sessionStore);
    }

    private HttpServletRequest authenticatedRequest(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("hp_session", "token-" + userId));
        when(sessionStore.getUserIdFromRequest(request)).thenReturn(userId);
        return request;
    }

    @Test
    void getMyLeagues_sans_session_renvoie_401() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(sessionStore.getUserIdFromRequest(request)).thenReturn(null);

        ResponseEntity<?> response = controller.getMyLeagues(request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void createLeague_renvoie_la_ligue_creee() {
        HttpServletRequest request = authenticatedRequest("u1");
        LeagueDto created = new LeagueDto("l1", "Ma ligue", "ABC123", 1, true, Instant.now());
        when(leagueService.createLeague("u1", "Ma ligue")).thenReturn(created);

        ResponseEntity<?> response = controller.createLeague(new CreateLeagueRequest("Ma ligue"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(created);
    }

    @Test
    void joinLeague_avec_un_code_invalide_renvoie_400() {
        HttpServletRequest request = authenticatedRequest("u1");
        when(leagueService.joinLeague("u1", "BADCODE"))
                .thenThrow(new IllegalArgumentException("Code d'invitation invalide."));

        ResponseEntity<?> response = controller.joinLeague(new JoinLeagueRequest("BADCODE"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void getLeagueMembers_pour_un_non_membre_renvoie_403() {
        HttpServletRequest request = authenticatedRequest("u1");
        when(leagueService.getMembers("l1", "u1"))
                .thenThrow(new IllegalStateException("Tu n'es pas membre de cette ligue."));

        ResponseEntity<?> response = controller.getLeagueMembers("l1", request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void getLeagueActivity_pour_un_non_membre_renvoie_403() {
        HttpServletRequest request = authenticatedRequest("u1");
        when(leagueService.getRecentActivity("l1", "u1"))
                .thenThrow(new IllegalStateException("Tu n'es pas membre de cette ligue."));

        ResponseEntity<?> response = controller.getLeagueActivity("l1", request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void getLeagueLeaderboard_pour_un_non_membre_renvoie_403() {
        HttpServletRequest request = authenticatedRequest("u1");
        when(leagueService.getLeagueLeaderboard("l1", "u1"))
                .thenThrow(new IllegalStateException("Tu n'es pas membre de cette ligue."));

        ResponseEntity<?> response = controller.getLeagueLeaderboard("l1", request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void leaveLeague_renvoie_204() {
        HttpServletRequest request = authenticatedRequest("u1");

        ResponseEntity<?> response = controller.leaveLeague("l1", request);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void previewLeague_avec_un_code_inconnu_renvoie_404() {
        HttpServletRequest request = authenticatedRequest("u1");
        when(leagueService.previewByCode("BADCODE"))
                .thenThrow(new IllegalArgumentException("Code d'invitation invalide."));

        ResponseEntity<?> response = controller.previewLeague("BADCODE", request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }
}
