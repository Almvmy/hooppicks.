package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.bankroll.BankrollService;
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
    @Mock
    private BankrollService bankrollService;
    @Mock
    private com.hooppicks.backendapplication.bet.PlayerPropsService playerPropsService;

    private BetController controller;

    @BeforeEach
    void setUp() {
        controller = new BetController(betRepository, userRepository, transactionRepository, sessionStore, matchRepository, bankrollService,
                playerPropsService);
        // Semaine de jeu commencée hier : elle finit dans 6 jours.
        lenient().when(bankrollService.currentWeekStart()).thenReturn(java.time.Instant.now().minus(java.time.Duration.ofDays(1)));
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
        t.setAbbreviation(name.substring(0, 3).toUpperCase());
        return t;
    }

    private Match scheduledMatch(String id) {
        Match m = new Match();
        m.setId(id);
        m.setStatus(MatchStatus.SCHEDULED);
        m.setDate(java.time.Instant.now().plus(java.time.Duration.ofHours(6)));
        m.setHomeTeam(team("Lakers"));
        m.setAwayTeam(team("Celtics"));
        m.setMoneylineHome(1.8);
        m.setMoneylineAway(2.1);
        return m;
    }

    private PlaceBetRequest.SelectionInput moneylineHome(String matchId) {
        return new PlaceBetRequest.SelectionInput(matchId, "Lakers vs Celtics", "moneyline", "home", "Lakers ML", 1.8, null, null);
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
                        new PlaceBetRequest.SelectionInput("m1", "Lakers vs Celtics", "moneyline", "away", "Celtics ML", 2.1, null, null)),
                10);
        ResponseEntity<?> response = controller.placeBet(body, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(betRepository, userRepository, transactionRepository, matchRepository);
    }

    @Test
    void pari_refuse_apres_le_coup_d_envoi_meme_si_le_statut_n_est_pas_encore_a_jour() {
        HttpServletRequest request = authenticatedRequest("u1");
        User user = user("u1", 100);
        when(userRepository.findByIdForUpdate("u1")).thenReturn(Optional.of(user));

        // Synchro en retard (ou API de matchs en panne) : toujours SCHEDULED,
        // mais le coup d'envoi date de 3 minutes.
        Match lagging = scheduledMatch("m1");
        lagging.setDate(java.time.Instant.now().minus(java.time.Duration.ofMinutes(3)));
        when(matchRepository.findById("m1")).thenReturn(Optional.of(lagging));

        PlaceBetRequest body = new PlaceBetRequest(List.of(moneylineHome("m1")), 10);
        ResponseEntity<?> response = controller.placeBet(body, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(user.getWalletBalance()).isEqualTo(100);
        verifyNoInteractions(betRepository, transactionRepository);
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
                new PlaceBetRequest.SelectionInput("m1", "Lakers vs Celtics", "moneyline", "home", "Lakers ML", 5.0, null, null);
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

    @Test
    void pari_refuse_sur_un_match_d_une_semaine_de_jeu_suivante() {
        HttpServletRequest request = authenticatedRequest("u1");
        User user = user("u1", 100);
        when(userRepository.findByIdForUpdate("u1")).thenReturn(Optional.of(user));
        Match nextWeek = scheduledMatch("m1");
        nextWeek.setDate(java.time.Instant.now().plus(java.time.Duration.ofDays(8)));
        when(matchRepository.findById("m1")).thenReturn(Optional.of(nextWeek));

        ResponseEntity<?> response = controller.placeBet(new PlaceBetRequest(List.of(moneylineHome("m1")), 10), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(user.getWalletBalance()).isEqualTo(100);
        verifyNoInteractions(betRepository);
    }

    @Test
    void les_libelles_du_ticket_viennent_du_match_pas_du_client() {
        HttpServletRequest request = authenticatedRequest("u1");
        when(userRepository.findByIdForUpdate("u1")).thenReturn(Optional.of(user("u1", 100)));
        Match match = scheduledMatch("m1");
        match.setSpreadValue(-3.5);
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));

        PlaceBetRequest body = new PlaceBetRequest(List.of(
                new PlaceBetRequest.SelectionInput("m1", "x", "spread", "away", "n'importe quoi", 9.9, null, null)), 10);
        ResponseEntity<?> response = controller.placeBet(body, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        var captor = org.mockito.ArgumentCaptor.forClass(Bet.class);
        verify(betRepository).save(captor.capture());
        var selection = captor.getValue().getSelections().get(0);
        assertThat(selection.getMatchLabel()).isEqualTo("Celtics vs Lakers");
        assertThat(selection.getLabel()).isEqualTo("CEL +3,5");
    }

    @Test
    void une_selection_inconnue_est_refusee() {
        HttpServletRequest request = authenticatedRequest("u1");
        when(userRepository.findByIdForUpdate("u1")).thenReturn(Optional.of(user("u1", 100)));
        when(matchRepository.findById("m1")).thenReturn(Optional.of(scheduledMatch("m1")));

        PlaceBetRequest body = new PlaceBetRequest(List.of(
                new PlaceBetRequest.SelectionInput("m1", "", "moneyline", "draw", "", 1.0, null, null)), 10);

        assertThat(controller.placeBet(body, request).getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(betRepository);
    }

    private PlaceBetRequest.SelectionInput brunsonOver(Double line) {
        return new PlaceBetRequest.SelectionInput("m1", "", "player_points", "over", "", 9.9, "p1", line);
    }

    @Test
    void pari_joueur_pose_avec_la_ligne_et_la_cote_du_serveur() {
        HttpServletRequest request = authenticatedRequest("u1");
        User user = user("u1", 1000);
        when(userRepository.findByIdForUpdate("u1")).thenReturn(Optional.of(user));
        Match match = scheduledMatch("m1");
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));
        when(playerPropsService.find(match, "player_points", "p1")).thenReturn(Optional.of(new com.hooppicks.backendapplication.bet.PlayerPropsService.PlayerProp(
                "player_points", "p1", "Jalen Brunson", "LAK", null, 26.5, 1.91, 1.91, 26.8)));

        ResponseEntity<?> response = controller.placeBet(new PlaceBetRequest(List.of(brunsonOver(26.5)), 100), request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        org.mockito.ArgumentCaptor<Bet> captor = org.mockito.ArgumentCaptor.forClass(Bet.class);
        verify(betRepository).save(captor.capture());
        com.hooppicks.backendapplication.entity.BetSelection sel = captor.getValue().getSelections().get(0);
        assertThat(sel.getOdds()).isEqualTo(1.91); // pas le 9,9 envoyé par le client
        assertThat(sel.getPropLine()).isEqualTo(26.5);
        assertThat(sel.getLabel()).isEqualTo("J. Brunson · Plus de 26,5 pts");
    }

    @Test
    void pari_joueur_rebonds_libelle_avec_son_unite() {
        HttpServletRequest request = authenticatedRequest("u1");
        when(userRepository.findByIdForUpdate("u1")).thenReturn(Optional.of(user("u1", 1000)));
        Match match = scheduledMatch("m1");
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));
        when(playerPropsService.find(match, "player_rebounds", "p2")).thenReturn(Optional.of(new com.hooppicks.backendapplication.bet.PlayerPropsService.PlayerProp(
                "player_rebounds", "p2", "Nikola Jokic", "DEN", null, 12.5, 1.91, 1.91, 12.7)));

        ResponseEntity<?> response = controller.placeBet(new PlaceBetRequest(List.of(
                new PlaceBetRequest.SelectionInput("m1", "", "player_rebounds", "under", "", 1.91, "p2", 12.5)), 100), request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        org.mockito.ArgumentCaptor<Bet> captor = org.mockito.ArgumentCaptor.forClass(Bet.class);
        verify(betRepository).save(captor.capture());
        assertThat(captor.getValue().getSelections().get(0).getLabel()).isEqualTo("N. Jokic · Moins de 12,5 rbd");
    }

    @Test
    void pari_joueur_refuse_si_la_ligne_a_bouge() {
        HttpServletRequest request = authenticatedRequest("u1");
        when(userRepository.findByIdForUpdate("u1")).thenReturn(Optional.of(user("u1", 1000)));
        Match match = scheduledMatch("m1");
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));
        when(playerPropsService.find(match, "player_points", "p1")).thenReturn(Optional.of(new com.hooppicks.backendapplication.bet.PlayerPropsService.PlayerProp(
                "player_points", "p1", "Jalen Brunson", "LAK", null, 27.5, 1.91, 1.91, 27.6)));

        ResponseEntity<?> response = controller.placeBet(new PlaceBetRequest(List.of(brunsonOver(26.5)), 100), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(String.valueOf(response.getBody())).contains("27,5");
        verify(betRepository, never()).save(any());
    }
}
