package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.WalletDataDto;
import com.hooppicks.backendapplication.dto.WalletTransactionDto;
import com.hooppicks.backendapplication.entity.User;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletTransactionRepository transactionRepository;
    @Mock
    private SessionStore sessionStore;

    private WalletController controller;

    @BeforeEach
    void setUp() {
        controller = new WalletController(userRepository, transactionRepository, sessionStore);
    }

    private HttpServletRequest authenticatedRequest(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("hp_session", "token-" + userId));
        when(sessionStore.getUserIdFromRequest(request)).thenReturn(userId);
        return request;
    }

    @Test
    void getWallet_sans_session_renvoie_401() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(sessionStore.getUserIdFromRequest(request)).thenReturn(null);

        ResponseEntity<WalletDataDto> response = controller.getWallet(request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void getWallet_renvoie_le_solde_de_l_utilisateur_connecte() {
        HttpServletRequest request = authenticatedRequest("u1");
        User user = new User();
        user.setId("u1");
        user.setWalletBalance(750);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        ResponseEntity<WalletDataDto> response = controller.getWallet(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().balance()).isEqualTo(750);
    }

    @Test
    void getTransactions_sans_session_renvoie_401() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(sessionStore.getUserIdFromRequest(request)).thenReturn(null);

        ResponseEntity<List<WalletTransactionDto>> response = controller.getTransactions(request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void getTransactions_ne_renvoie_que_celles_de_l_utilisateur_connecte() {
        HttpServletRequest request = authenticatedRequest("u1");
        when(transactionRepository.findByUserIdOrderByDateDesc("u1")).thenReturn(List.of());

        ResponseEntity<List<WalletTransactionDto>> response = controller.getTransactions(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEmpty();
    }
}
