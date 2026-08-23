package com.hooppicks.backendapplication.security;

import com.hooppicks.backendapplication.email.EmailService;
import com.hooppicks.backendapplication.entity.EmailVerificationToken;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.EmailVerificationTokenRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final long TOKEN_TTL_MINUTES = 24 * 60;
    private static final int MAX_REQUESTS_PER_WINDOW = 3;
    private static final long REQUEST_WINDOW_MINUTES = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private record RequestCount(AtomicInteger count, Instant windowStart) {}

    // Même esprit que PasswordResetService : évite qu'un renvoi de lien de
    // vérification puisse être utilisé pour bombarder la boîte mail de qqn.
    private final ConcurrentHashMap<String, RequestCount> requestsByUserId = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailVerificationService(UserRepository userRepository, EmailVerificationTokenRepository tokenRepository,
                                     EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void requestVerification(User user) {
        if (user.isEmailVerified() || isRateLimited(user.getId())) return;

        tokenRepository.deleteByUserId(user.getId()); // un seul token valide à la fois

        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setToken(generateToken());
        verificationToken.setExpiresAt(Instant.now().plusSeconds(TOKEN_TTL_MINUTES * 60));
        tokenRepository.save(verificationToken);

        String verifyLink = frontendUrl + "/verify-email?token=" + verificationToken.getToken();
        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), verifyLink);
        } catch (Exception e) {
            // Ne bloque jamais l'inscription : l'utilisateur pourra redemander
            // le lien plus tard depuis l'app si l'envoi échoue (SMTP down...).
            log.warn("Échec d'envoi de l'e-mail de vérification", e);
        }
    }

    /**
     * @return true si le token était valide et l'email a été marqué vérifié
     */
    @Transactional
    public boolean verify(String token) {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) return false;

        EmailVerificationToken verificationToken = tokenOpt.get();
        if (Instant.now().isAfter(verificationToken.getExpiresAt())) return false;

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);
        return true;
    }

    private boolean isRateLimited(String userId) {
        RequestCount current = requestsByUserId.compute(userId, (key, existing) -> {
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
