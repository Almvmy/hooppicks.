package com.hooppicks.backendapplication.controller;

import com.hooppicks.backendapplication.dto.ChangeEmailRequest;
import com.hooppicks.backendapplication.dto.ChangePasswordRequest;
import com.hooppicks.backendapplication.dto.DeleteAccountRequest;
import com.hooppicks.backendapplication.dto.ForgotPasswordRequest;
import com.hooppicks.backendapplication.dto.LoginRequest;
import com.hooppicks.backendapplication.dto.RegisterRequest;
import com.hooppicks.backendapplication.dto.ResetPasswordRequest;
import com.hooppicks.backendapplication.dto.UpdateNotificationPreferencesRequest;
import com.hooppicks.backendapplication.dto.UpdateProfileRequest;
import com.hooppicks.backendapplication.dto.UserProfileDto;
import com.hooppicks.backendapplication.entity.User;
import com.hooppicks.backendapplication.repository.BetRepository;
import com.hooppicks.backendapplication.repository.UserRepository;
import com.hooppicks.backendapplication.security.AccountDeletionService;
import com.hooppicks.backendapplication.security.PasswordResetService;
import com.hooppicks.backendapplication.security.SessionStore;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import com.hooppicks.backendapplication.security.LoginAttemptService;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/auth")

public class AuthController {

    private static final Set<String> VALID_POSITIONS = Set.of("PG", "SG", "SF", "PF", "C");
    private static final Set<String> VALID_COLORWAYS =
            Set.of("orange", "purple", "blue", "green", "red", "teal");
    private static final Set<String> VALID_ICONS =
            Set.of("dunk", "three", "handles", "defense", "playmaker");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionStore sessionStore;
    private final BetRepository betRepository;
    private final LoginAttemptService loginAttemptService;
    private final PasswordResetService passwordResetService;
    private final AccountDeletionService accountDeletionService;

    @org.springframework.beans.factory.annotation.Value("${app.cookie-same-site:Lax}")
    private String cookieSameSite;

    @org.springframework.beans.factory.annotation.Value("${app.cookie-secure:false}")
    private boolean cookieSecure;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          SessionStore sessionStore, BetRepository betRepository,
                          LoginAttemptService loginAttemptService, PasswordResetService passwordResetService,
                          AccountDeletionService accountDeletionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionStore = sessionStore;
        this.betRepository = betRepository;
        this.loginAttemptService = loginAttemptService;
        this.passwordResetService = passwordResetService;
        this.accountDeletionService = accountDeletionService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserProfileDto> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.status(409).build();
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        setSessionCookie(response, user.getId());
        return ResponseEntity.ok(buildProfileDto(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        if (loginAttemptService.isBlocked(request.email())) {
            return ResponseEntity.status(429).body("Trop de tentatives. Réessaie dans 15 minutes.");
        }

        User user = userRepository.findByEmail(request.email()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailedAttempt(request.email());
            return ResponseEntity.status(401).build();
        }

        loginAttemptService.recordSuccessfulLogin(request.email());
        setSessionCookie(response, user.getId());
        return ResponseEntity.ok(buildProfileDto(user));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> me(HttpServletRequest request) {
        String userId = sessionStore.getUserIdFromRequest(request);
        if (userId == null) return ResponseEntity.status(401).build();

        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(buildProfileDto(user)))
                .orElse(ResponseEntity.status(401).build());
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserProfileDto> updateProfile(
            @RequestBody UpdateProfileRequest request, HttpServletRequest httpRequest) {
        String userId = sessionStore.getUserIdFromRequest(httpRequest);
        if (userId == null) return ResponseEntity.status(401).build();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        if (request.favoriteTeam() != null) {
            user.setFavoriteTeam(request.favoriteTeam());
        }
        if (request.avatarNumber() != null) {
            user.setAvatarNumber(Math.max(0, Math.min(99, request.avatarNumber())));
        }
        if (request.avatarPosition() != null && VALID_POSITIONS.contains(request.avatarPosition())) {
            user.setAvatarPosition(request.avatarPosition());
        }
        if (request.avatarColorway() != null && VALID_COLORWAYS.contains(request.avatarColorway())) {
            user.setAvatarColorway(request.avatarColorway());
        }
        if (request.avatarIcon() != null && VALID_ICONS.contains(request.avatarIcon())) {
            user.setAvatarIcon(request.avatarIcon());
        }

        userRepository.save(user);
        return ResponseEntity.ok(buildProfileDto(user));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        // Toujours 200, que l'email corresponde à un compte ou non — pas d'énumération.
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean success = passwordResetService.resetPassword(request.token(), request.newPassword());
        if (!success) {
            return ResponseEntity.badRequest().body("Lien invalide ou expiré.");
        }
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/notification-preferences")
    public ResponseEntity<UserProfileDto> updateNotificationPreferences(
            @RequestBody UpdateNotificationPreferencesRequest request, HttpServletRequest httpRequest) {
        String userId = sessionStore.getUserIdFromRequest(httpRequest);
        if (userId == null) return ResponseEntity.status(401).build();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        user.setNotifyMatchStarting(request.notifyMatchStarting());
        user.setNotifyBetResults(request.notifyBetResults());
        user.setNotifyLeagueActivity(request.notifyLeagueActivity());
        userRepository.save(user);

        return ResponseEntity.ok(buildProfileDto(user));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        String userId = sessionStore.getUserIdFromRequest(httpRequest);
        if (userId == null) return ResponseEntity.status(401).build();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body("Mot de passe actuel incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Comme pour le reset par lien : si le mot de passe a fuité, on ne veut
        // pas qu'une session déjà ouverte par quelqu'un d'autre reste valide.
        sessionStore.invalidateAllForUser(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-email")
    public ResponseEntity<?> changeEmail(
            @Valid @RequestBody ChangeEmailRequest request, HttpServletRequest httpRequest) {
        String userId = sessionStore.getUserIdFromRequest(httpRequest);
        if (userId == null) return ResponseEntity.status(401).build();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body("Mot de passe incorrect.");
        }

        if (!request.newEmail().equalsIgnoreCase(user.getEmail())
                && userRepository.findByEmail(request.newEmail()).isPresent()) {
            return ResponseEntity.status(409).body("Cet email est déjà utilisé.");
        }

        user.setEmail(request.newEmail());
        userRepository.save(user);

        return ResponseEntity.ok(buildProfileDto(user));
    }

    @PostMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request, HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String userId = sessionStore.getUserIdFromRequest(httpRequest);
        if (userId == null) return ResponseEntity.status(401).build();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body("Mot de passe incorrect.");
        }

        accountDeletionService.deleteAccount(userId);

        ResponseCookie expired = ResponseCookie.from("hp_session", "").path("/").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("hp_session")) {
                    sessionStore.invalidate(cookie.getValue());
                }
            }
        }
        ResponseCookie expired = ResponseCookie.from("hp_session", "")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expired.toString());
        return ResponseEntity.ok().build();
    }

    private UserProfileDto buildProfileDto(User user) {
        List<Object[]> stats = betRepository.getUserStats(user.getId());
        long totalBets = 0;
        long wonBets = 0;
        if (!stats.isEmpty() && stats.get(0)[0] != null) {
            totalBets = (Long) stats.get(0)[0];
            wonBets = stats.get(0)[1] != null ? (Long) stats.get(0)[1] : 0;
        }
        int winRate = totalBets == 0 ? 0 : (int) Math.round((wonBets * 100.0) / totalBets);
        return UserProfileDto.from(user, winRate, (int) totalBets);
    }

    private void setSessionCookie(HttpServletResponse response, String userId) {
        String token = sessionStore.createSession(userId);
        ResponseCookie cookie = ResponseCookie.from("hp_session", token)
                .path("/")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .maxAge(60 * 60 * 24)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}