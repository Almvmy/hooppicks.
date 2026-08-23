package com.hooppicks.backendapplication.security;

import com.hooppicks.backendapplication.email.EmailService;
import com.hooppicks.backendapplication.entity.PasswordResetToken;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.PasswordResetTokenRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private SessionStore sessionStore;
    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, tokenRepository, emailService, sessionStore, passwordEncoder);
    }

    private User user(String id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setUsername("joueur");
        return u;
    }

    private PasswordResetToken token(User user, String value, boolean used, Instant expiresAt) {
        PasswordResetToken t = new PasswordResetToken();
        t.setUser(user);
        t.setToken(value);
        t.setUsed(used);
        t.setExpiresAt(expiresAt);
        return t;
    }

    @Test
    void requestReset_avec_un_email_connu_cree_un_token_et_envoie_le_mail() {
        User user = user("u1", "joueur@example.com");
        when(userRepository.findByEmail("joueur@example.com")).thenReturn(Optional.of(user));

        service.requestReset("joueur@example.com");

        verify(tokenRepository).deleteByUserId("u1");
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isNotBlank();
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        verify(emailService).sendPasswordResetEmail(eq("joueur@example.com"), eq("joueur"), any());
    }

    @Test
    void requestReset_avec_un_email_inconnu_ne_cree_rien_mais_ne_leve_pas_d_erreur() {
        when(userRepository.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        service.requestReset("inconnu@example.com");

        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void requestReset_bloque_apres_3_demandes_dans_la_fenetre_pour_le_meme_email() {
        User user = user("u1", "joueur@example.com");
        when(userRepository.findByEmail("joueur@example.com")).thenReturn(Optional.of(user));

        service.requestReset("joueur@example.com");
        service.requestReset("joueur@example.com");
        service.requestReset("joueur@example.com");
        // 4e appel dans la même fenêtre : doit être bloqué avant même de toucher au repository
        service.requestReset("joueur@example.com");

        verify(tokenRepository, times(3)).save(any());
    }

    @Test
    void resetPassword_avec_un_token_valide_change_le_mot_de_passe_et_invalide_les_sessions() {
        User user = user("u1", "joueur@example.com");
        PasswordResetToken resetToken = token(user, "abc", false, Instant.now().plusSeconds(600));
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("nouveauMotDePasse")).thenReturn("hash-encode");

        boolean result = service.resetPassword("abc", "nouveauMotDePasse");

        assertThat(result).isTrue();
        assertThat(user.getPasswordHash()).isEqualTo("hash-encode");
        assertThat(resetToken.isUsed()).isTrue();
        verify(sessionStore).invalidateAllForUser("u1");
    }

    @Test
    void resetPassword_avec_un_token_inconnu_echoue_sans_toucher_a_rien() {
        when(tokenRepository.findByToken("inconnu")).thenReturn(Optional.empty());

        boolean result = service.resetPassword("inconnu", "nouveauMotDePasse");

        assertThat(result).isFalse();
        verifyNoInteractions(sessionStore, passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_avec_un_token_deja_utilise_echoue() {
        User user = user("u1", "joueur@example.com");
        PasswordResetToken resetToken = token(user, "abc", true, Instant.now().plusSeconds(600));
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(resetToken));

        boolean result = service.resetPassword("abc", "nouveauMotDePasse");

        assertThat(result).isFalse();
        verifyNoInteractions(sessionStore, passwordEncoder);
    }

    @Test
    void resetPassword_avec_un_token_expire_echoue() {
        User user = user("u1", "joueur@example.com");
        PasswordResetToken resetToken = token(user, "abc", false, Instant.now().minusSeconds(60));
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(resetToken));

        boolean result = service.resetPassword("abc", "nouveauMotDePasse");

        assertThat(result).isFalse();
        verifyNoInteractions(sessionStore, passwordEncoder);
    }
}
