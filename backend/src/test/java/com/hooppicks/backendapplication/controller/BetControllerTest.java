package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.PlaceBetRequest;
import com.hooppicks.backendapplication.entity.Bet;
import com.hooppicks.backendapplication.entity.Match;
import com.hooppicks.backendapplication.entity.MatchStatus;
import com.hooppicks.backendapplication.entity.Team;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.MatchRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.repository.WalletTransactionRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetControllerTest {

    @Mock
    private BetRepository betRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletTransactionRepository transactionRepository;
    @Mock
    private SessionStore sessionStore;
    @Mock
    private MatchRepository matchRepository;

    private BetController controller;

    @BeforeEach
    void setUp() {
        controller = new BetController(betRepository, userRepository, transactionRepository, sessionStore, matchRepository);
    }

    private HttpServletRequest authenticatedRequest(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("hp_session", "token-" + userId));
        when(sessionStore.getUserIdFromRequest(request)).thenReturn(userId);
        return request;
    }

    private User user(String id, int balance) {
        User u = new User();
        u.setId(id);
        u.setWalletBalance(balance);
        return u;
    }

    private Team team(String name) {
        Team t = new Team();
        t.setName(name);
        return t;
    }

    private Match scheduledMatch(String id) {
        Match m = new Match();
        m.setId(id);
        m.setStatus(MatchStatus.SCHEDULED);
        m.setHomeTeam(team("Lakers"));
        m.setAwayTeam(team("Celtics"));
        m.setMoneylineHome(1.8);
        m.setMoneylineAway(2.1);
        return m;
    }

    private PlaceBetRequest.SelectionInput moneylineHome(String matchId) {
        return new PlaceBetRequest.SelectionInput(matchId, "Lakers vs Celtics", "moneyline", "home", "Lakers ML", 1.8);
    }

    @Test
    void pari_refuse_sans_session() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(sessionStore.getUserIdFromRequest(request)).thenReturn(null);

        PlaceBetRequest body = new PlaceBetRequest(List.of(moneylineHome("m1")), 10);
        ResponseEntity<?> response = controller.placeBet(body, request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        verifyNoInteractions(betRepository, matchRepository, transactionRepository);
    }

    @Test
    void pari_refuse_si_le_solde_est_insuffisant() {
        HttpServletRequest request = authenticatedRequest("u1");
        User user = user("u1", 5);
        when(userRepository.findByIdForUpdate("u1")).thenReturn(Optional.of(user));

        PlaceBetRequest body = new PlaceBetRequest(List.of(moneylineHome("m1")), 10);
        ResponseEntity<?> response = controller.placeBet(body, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(user.getWalletBalance()).isEqualTo(5); // rien débité
        verifyNoInteractions(betRepository, transactionRepository, matchRepository);
    }

    @Test
    void pari_refuse_si_le_meme_match_apparait_deux_fois_dans_le_ticket() {
        HttpServletRequest request = authenticatedRequest("u1");

        PlaceBetRequest body = new PlaceBetRequest(
                List.of(moneylineHome("m1"),
                        new PlaceBetRequest.SelectionInput("m1", "Lakers vs Celtics", "moneyline", "away", "Celtics ML", 2.1)),
                10);
        ResponseEntity<?> response = controller.placeBet(body, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(betRepository, userRepository, transactionRepository, matchRepository);
    }

    @Test
    void pari_refuse_si_le_match_n_est_plus_ouvert() {
        HttpServletRequest request = authenticatedRequest("u1");
        User user = user("u1", 100);
        when(userRepository.findByIdForUpdate("u1")).thenReturn(Optional.of(user));

        Match started = scheduledMatch("m1");
        started.setStatus(MatchStatus.LIVE);
        when(matchRepository.findById("m1")).thenReturn(Optional.of(started));

        PlaceBetRequest body = new PlaceBetRequest(List.of(moneylineHome("m1")), 10);
        ResponseEntity<?> response = controller.placeBet(body, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(user.getWalletBalance()).isEqualTo(100);
        verifyNoInteractions(betRepository, transactionRepository);
    }

    @Test
    void pari_accepte_debite_le_solde_en_utilisant_la_cote_serveur_pas_celle_du_client() {
        HttpServletRequest request = authenticatedRequest("u1");
        User user = user("u1", 100);
        when(userRepository.findByIdForUpdate("u1")).thenReturn(Optional.of(user));

        Match match = scheduledMatch("m1"); // cote serveur réelle : 1.8
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));

        // Le client envoie une ancienne cote plus favorable (5.0) : doit être ignorée.
        PlaceBetRequest.SelectionInput staleOdds =
                new PlaceBetRequest.SelectionInput("m1", "Lakers vs Celtics", "moneyline", "home", "Lakers ML", 5.0);
        PlaceBetRequest body = new PlaceBetRequest(List.of(staleOdds), 10);

        ResponseEntity<?> response = controller.placeBet(body, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(user.getWalletBalance()).isEqualTo(90); // 100 - 10, débité une seule fois

        verify(betRepository).save(any(Bet.class));
        verify(userRepository).save(user);
        verify(transactionRepository).save(any());
    }

    @Test
    void pari_refuse_si_le_match_est_introuvable() {
        HttpServletRequest request = authenticatedRequest("u1");
        User user = user("u1", 100);
        when(userRepository.findByIdForUpdate("u1")).thenReturn(Optional.of(user));
        when(matchRepository.findById("m1")).thenReturn(Optional.empty());

        PlaceBetRequest body = new PlaceBetRequest(List.of(moneylineHome("m1")), 10);
        ResponseEntity<?> response = controller.placeBet(body, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(betRepository, transactionRepository);
    }
}
