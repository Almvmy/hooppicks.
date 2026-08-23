package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.*;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.security.AccountDeletionService;
import com.hooppicks.backendapplication.security.EmailVerificationService;
import com.hooppicks.backendapplication.security.LoginAttemptService;
import com.hooppicks.backendapplication.security.PasswordResetService;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SessionStore sessionStore;
    @Mock
    private BetRepository betRepository;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private PasswordResetService passwordResetService;
    @Mock
    private AccountDeletionService accountDeletionService;
    @Mock
    private EmailVerificationService emailVerificationService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(userRepository, passwordEncoder, sessionStore, betRepository,
                loginAttemptService, passwordResetService, accountDeletionService, emailVerificationService);
        lenient().when(betRepository.getUserStats(any())).thenReturn(Collections.emptyList());
    }

    private User user(String id, String email, String username, String passwordHash) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setUsername(username);
        u.setPasswordHash(passwordHash);
        return u;
    }

    private HttpServletRequest authenticatedRequest(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("hp_session", "token-" + userId));
        lenient().when(sessionStore.getUserIdFromRequest(request)).thenReturn(userId);
        return request;
    }

    @Test
    void register_avec_un_email_deja_pris_renvoie_409() {
        when(userRepository.findByEmail("joueur@example.com")).thenReturn(Optional.of(user("u1", "joueur@example.com", "joueur", "hash")));

        RegisterRequest body = new RegisterRequest("joueur", "joueur@example.com", "motdepasse");
        ResponseEntity<?> response = controller.register(body, new MockHttpServletResponse());

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_cree_le_compte_pose_le_cookie_et_declenche_la_verification_email() {
        when(userRepository.findByEmail("joueur@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("motdepasse")).thenReturn("hash");
        when(sessionStore.createSession(any())).thenReturn("token-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        RegisterRequest body = new RegisterRequest("joueur", "joueur@example.com", "motdepasse");
        ResponseEntity<?> result = controller.register(body, response);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        verify(userRepository).save(any(User.class));
        verify(emailVerificationService).requestVerification(any(User.class));
        assertThat(response.getHeader("Set-Cookie")).contains("hp_session=token-abc");
    }

    @Test
    void login_avec_un_email_bloque_renvoie_429_sans_verifier_le_mot_de_passe() {
        when(loginAttemptService.isBlocked("joueur@example.com")).thenReturn(true);

        LoginRequest body = new LoginRequest("joueur@example.com", "motdepasse");
        ResponseEntity<?> response = controller.login(body, new MockHttpServletResponse());

        assertThat(response.getStatusCode().value()).isEqualTo(429);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void login_avec_un_mauvais_mot_de_passe_enregistre_la_tentative_et_renvoie_401() {
        when(loginAttemptService.isBlocked("joueur@example.com")).thenReturn(false);
        User user = user("u1", "joueur@example.com", "joueur", "hash");
        when(userRepository.findByEmail("joueur@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("mauvais", "hash")).thenReturn(false);

        LoginRequest body = new LoginRequest("joueur@example.com", "mauvais");
        ResponseEntity<?> response = controller.login(body, new MockHttpServletResponse());

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        verify(loginAttemptService).recordFailedAttempt("joueur@example.com");
    }

    @Test
    void login_valide_pose_le_cookie_et_reinitialise_le_compteur_de_tentatives() {
        when(loginAttemptService.isBlocked("joueur@example.com")).thenReturn(false);
        User user = user("u1", "joueur@example.com", "joueur", "hash");
        when(userRepository.findByEmail("joueur@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("motdepasse", "hash")).thenReturn(true);
        when(sessionStore.createSession("u1")).thenReturn("token-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        LoginRequest body = new LoginRequest("joueur@example.com", "motdepasse");
        ResponseEntity<?> result = controller.login(body, response);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        verify(loginAttemptService).recordSuccessfulLogin("joueur@example.com");
        assertThat(response.getHeader("Set-Cookie")).contains("hp_session=token-abc");
    }

    @Test
    void me_sans_session_renvoie_401() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(sessionStore.getUserIdFromRequest(request)).thenReturn(null);

        ResponseEntity<UserProfileDto> response = controller.me(request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void updateProfile_ignore_les_valeurs_hors_liste_fermee() {
        HttpServletRequest request = authenticatedRequest("u1");
        User user = user("u1", "joueur@example.com", "joueur", "hash");
        user.setAvatarPosition("PG");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        UpdateProfileRequest body = new UpdateProfileRequest(null, null, "INVALID_POSITION", null, null);
        controller.updateProfile(body, request);

        assertThat(user.getAvatarPosition()).isEqualTo("PG"); // inchangé
    }

    @Test
    void updateProfile_applique_les_valeurs_valides() {
        HttpServletRequest request = authenticatedRequest("u1");
        User user = user("u1", "joueur@example.com", "joueur", "hash");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        UpdateProfileRequest body = new UpdateProfileRequest("Lakers", 23, "SG", "blue", "three");
        controller.updateProfile(body, request);

        assertThat(user.getFavoriteTeam()).isEqualTo("Lakers");
        assertThat(user.getAvatarNumber()).isEqualTo(23);
        assertThat(user.getAvatarPosition()).isEqualTo("SG");
        assertThat(user.getAvatarColorway()).isEqualTo("blue");
        assertThat(user.getAvatarIcon()).isEqualTo("three");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_avec_le_mauvais_mot_de_passe_actuel_echoue() {
        HttpServletRequest request = authenticatedRequest("u1");
        User user = user("u1", "joueur@example.com", "joueur", "hash");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("mauvais", "hash")).thenReturn(false);

        ChangePasswordRequest body = new ChangePasswordRequest("mauvais", "nouveauMotDePasse");
        ResponseEntity<?> response = controller.changePassword(body, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verify(userRepository, never()).save(any());
        verify(sessionStore, never()).invalidateAllForUser(any());
    }

    @Test
    void changePassword_valide_invalide_toutes_les_sessions_actives() {
        HttpServletRequest request = authenticatedRequest("u1");
        User user = user("u1", "joueur@example.com", "joueur", "hash");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("ancien", "hash")).thenReturn(true);
        when(passwordEncoder.encode("nouveau1234")).thenReturn("nouveau-hash");

        ChangePasswordRequest body = new ChangePasswordRequest("ancien", "nouveau1234");
        ResponseEntity<?> response = controller.changePassword(body, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(user.getPasswordHash()).isEqualTo("nouveau-hash");
        verify(sessionStore).invalidateAllForUser("u1");
    }

    @Test
    void changeEmail_vers_un_email_deja_pris_renvoie_409() {
        HttpServletRequest request = authenticatedRequest("u1");
        User user = user("u1", "joueur@example.com", "joueur", "hash");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("motdepasse", "hash")).thenReturn(true);
        when(userRepository.findByEmail("pris@example.com"))
                .thenReturn(Optional.of(user("u2", "pris@example.com", "autre", "hash2")));

        ChangeEmailRequest body = new ChangeEmailRequest("pris@example.com", "motdepasse");
        ResponseEntity<?> response = controller.changeEmail(body, request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void changeEmail_valide_remet_emailVerified_a_false_et_redemande_la_verification() {
        HttpServletRequest request = authenticatedRequest("u1");
        User user = user("u1", "ancien@example.com", "joueur", "hash");
        user.setEmailVerified(true);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("motdepasse", "hash")).thenReturn(true);
        when(userRepository.findByEmail("nouveau@example.com")).thenReturn(Optional.empty());

        ChangeEmailRequest body = new ChangeEmailRequest("nouveau@example.com", "motdepasse");
        controller.changeEmail(body, request);

        assertThat(user.getEmail()).isEqualTo("nouveau@example.com");
        assertThat(user.isEmailVerified()).isFalse();
        verify(emailVerificationService).requestVerification(user);
    }

    @Test
    void deleteAccount_avec_le_bon_mot_de_passe_supprime_le_compte_et_expire_le_cookie() {
        HttpServletRequest request = authenticatedRequest("u1");
        User user = user("u1", "joueur@example.com", "joueur", "hash");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("motdepasse", "hash")).thenReturn(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        DeleteAccountRequest body = new DeleteAccountRequest("motdepasse");
        ResponseEntity<?> result = controller.deleteAccount(body, request, response);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        verify(accountDeletionService).deleteAccount("u1");
        assertThat(response.getHeader("Set-Cookie")).contains("Max-Age=0");
    }

    @Test
    void logout_invalide_la_session_et_expire_le_cookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("hp_session", "token-abc"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.logout(request, response);

        verify(sessionStore).invalidate("token-abc");
        assertThat(response.getHeader("Set-Cookie")).contains("Max-Age=0");
    }
}
