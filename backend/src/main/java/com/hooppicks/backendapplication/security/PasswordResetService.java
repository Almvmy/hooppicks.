package com.hooppicks.backendapplication.security;

import com.hooppicks.backendapplication.email.EmailService;
import com.hooppicks.backendapplication.entity.PasswordResetToken;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.PasswordResetTokenRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PasswordResetService {

    private static final long TOKEN_TTL_MINUTES = 30;
    private static final int MAX_REQUESTS_PER_WINDOW = 3;
    private static final long REQUEST_WINDOW_MINUTES = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private record RequestCount(AtomicInteger count, Instant windowStart) {}

    // Anti-spam sur les demandes de reset : évite qu'on bombarde la boîte mail
    // d'un utilisateur. Même esprit que LoginAttemptService, fenêtre différente.
    private final ConcurrentHashMap<String, RequestCount> requestsByEmail = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final SessionStore sessionStore;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public PasswordResetService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository,
                                 EmailService emailService, SessionStore sessionStore,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.sessionStore = sessionStore;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Ne révèle jamais si l'email correspond à un compte — même comportement
     * qu'un email inconnu ou connu, pour ne pas permettre l'énumération de
     * comptes via ce formulaire (contrairement à /register, où c'est accepté).
     */
    @Transactional
    public void requestReset(String email) {
        if (isRateLimited(email)) return;

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();
        tokenRepository.deleteByUserId(user.getId()); // un seul token valide à la fois

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(generateToken());
        resetToken.setExpiresAt(Instant.now().plusSeconds(TOKEN_TTL_MINUTES * 60));
        tokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + resetToken.getToken();
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetLink);
        } catch (Exception e) {
            // Ne jamais laisser un échec d'envoi (SMTP mal configuré, panne du
            // fournisseur...) se distinguer d'un email inconnu côté client —
            // sinon le code de réponse redevient un oracle d'énumération.
            e.printStackTrace();
        }
    }

    /**
     * @return true si le token était valide et le mot de passe a été changé
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) return false;

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.isUsed() || Instant.now().isAfter(resetToken.getExpiresAt())) return false;

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        // Si le mot de passe a fuité, on ne veut pas qu'une session déjà ouverte
        // (potentiellement par quelqu'un d'autre) reste valide après le reset.
        sessionStore.invalidateAllForUser(user.getId());

        return true;
    }

    private boolean isRateLimited(String email) {
        RequestCount current = requestsByEmail.compute(email.toLowerCase(), (key, existing) -> {
            if (existing == null || Instant.now().isAfter(existing.windowStart().plusSeconds(REQUEST_WINDOW_MINUTES * 60))) {
                return new RequestCount(new AtomicInteger(1), Instant.now());
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return current.count().get() > MAX_REQUESTS_PER_WINDOW;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
