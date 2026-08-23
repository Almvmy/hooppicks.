package com.hooppicks.backendapplication.security;

import com.hooppicks.backendapplication.email.EmailService;
import com.hooppicks.backendapplication.entity.EmailVerificationToken;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.EmailVerificationTokenRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailVerificationTokenRepository tokenRepository;
    @Mock
    private EmailService emailService;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(userRepository, tokenRepository, emailService);
    }

    private User user(String id, boolean verified) {
        User u = new User();
        u.setId(id);
        u.setEmail("joueur@example.com");
        u.setUsername("joueur");
        u.setEmailVerified(verified);
        return u;
    }

    private EmailVerificationToken token(User user, String value, Instant expiresAt) {
        EmailVerificationToken t = new EmailVerificationToken();
        t.setUser(user);
        t.setToken(value);
        t.setExpiresAt(expiresAt);
        return t;
    }

    @Test
    void requestVerification_cree_un_token_et_envoie_le_mail() {
        User user = user("u1", false);

        service.requestVerification(user);

        verify(tokenRepository).deleteByUserId("u1");
        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isNotBlank();
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        verify(emailService).sendVerificationEmail(eq("joueur@example.com"), eq("joueur"), any());
    }

    @Test
    void requestVerification_ne_fait_rien_si_deja_verifie() {
        User user = user("u1", true);

        service.requestVerification(user);

        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void requestVerification_bloque_apres_3_demandes_dans_la_fenetre() {
        User user = user("u1", false);

        service.requestVerification(user);
        service.requestVerification(user);
        service.requestVerification(user);
        // 4e appel dans la même fenêtre : doit être bloqué avant même de toucher au repository
        service.requestVerification(user);

        verify(tokenRepository, times(3)).save(any());
    }

    @Test
    void verify_avec_un_token_valide_marque_l_email_verifie_et_supprime_le_token() {
        User user = user("u1", false);
        EmailVerificationToken verificationToken = token(user, "abc", Instant.now().plusSeconds(600));
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(verificationToken));

        boolean result = service.verify("abc");

        assertThat(result).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).delete(verificationToken);
    }

    @Test
    void verify_avec_un_token_inconnu_echoue() {
        when(tokenRepository.findByToken("inconnu")).thenReturn(Optional.empty());

        boolean result = service.verify("inconnu");

        assertThat(result).isFalse();
        verifyNoInteractions(userRepository);
    }

    @Test
    void verify_avec_un_token_expire_echoue() {
        User user = user("u1", false);
        EmailVerificationToken verificationToken = token(user, "abc", Instant.now().minusSeconds(60));
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(verificationToken));

        boolean result = service.verify("abc");

        assertThat(result).isFalse();
        assertThat(user.isEmailVerified()).isFalse();
        verifyNoInteractions(userRepository);
    }
}
