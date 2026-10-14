package com.hooppicks.backendapplication.security;

import com.hooppicks.backendapplication.entity.UserSession;
import com.hooppicks.backendapplication.repository.UserSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sessions en base (`UserSession`), plus en mémoire : avant, chaque
 * redéploiement déconnectait tout le monde, et une session ne vivait que
 * 24 h. Elle dure maintenant 60 jours et se prolonge à l'usage (au plus une
 * écriture par jour et par session) : un joueur qui ouvre l'app au moins une
 * fois tous les deux mois reste connecté.
 *
 * Chaque requête vérifie la session : sa lecture est gardée 60 s en mémoire
 * pour ne pas interroger la base à chaque appel. Déconnexion et changement
 * de mot de passe vident aussi ce cache, l'effet reste immédiat.
 */
@Component
public class SessionStore {

    private static final Logger log = LoggerFactory.getLogger(SessionStore.class);

    public static final Duration SESSION_TTL = Duration.ofDays(60);
    /** Une session n'est prolongée (écrite en base) qu'une fois par jour au plus. */
    static final Duration RENEW_AFTER = Duration.ofDays(1);
    static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private static final SecureRandom RANDOM = new SecureRandom();

    private record Cached(String userId, Instant expiresAt, Instant lastSeenAt, Instant cachedAt) {}

    private final UserSessionRepository repository;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public SessionStore(UserSessionRepository repository) {
        this.repository = repository;
    }

    public String createSession(String userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        UserSession session = new UserSession();
        session.setTokenHash(hash(token));
        session.setUserId(userId);
        session.setExpiresAt(Instant.now().plus(SESSION_TTL));
        repository.save(session);
        return token;
    }

    public String getUserId(String token) {
        Cached session = lookup(token);
        return session == null ? null : session.userId();
    }

    /**
     * Prolonge la session si sa dernière prolongation date d'un jour ou plus.
     * Renvoie true dans ce cas : le cookie doit alors être reposé avec une
     * nouvelle durée (cf. SessionRenewalFilter).
     */
    public boolean renewIfDue(String token) {
        Cached session = lookup(token);
        if (session == null) return false;
        Instant now = Instant.now();
        if (session.lastSeenAt().plus(RENEW_AFTER).isAfter(now)) return false;

        String key = hash(token);
        return repository.findById(key).map(s -> {
            s.setLastSeenAt(now);
            s.setExpiresAt(now.plus(SESSION_TTL));
            repository.save(s);
            cache.put(key, new Cached(s.getUserId(), s.getExpiresAt(), now, now));
            return true;
        }).orElse(false);
    }

    public void invalidate(String token) {
        String key = hash(token);
        cache.remove(key);
        repository.deleteById(key);
    }

    /**
     * Invalide toutes les sessions actives d'un utilisateur : utilisé après un
     * changement de mot de passe (reset), pour ne pas laisser valide une session
     * déjà ouverte par quelqu'un d'autre si le compte a été compromis.
     */
    public void invalidateAllForUser(String userId) {
        cache.values().removeIf(c -> c.userId().equals(userId));
        repository.deleteAllForUser(userId);
    }

    public String getUserIdFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        String token = tokenFrom(request);
        return token == null ? null : getUserId(token);
    }

    public static String tokenFrom(jakarta.servlet.http.HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals("hp_session")) return cookie.getValue();
        }
        return null;
    }

    @Scheduled(cron = "0 45 4 * * *", zone = "GMT")
    public void purgeExpired() {
        int deleted = repository.deleteExpired(Instant.now());
        if (deleted > 0) log.info("{} session(s) expirée(s) supprimée(s)", deleted);
        cache.clear();
    }

    private Cached lookup(String token) {
        if (token == null || token.isBlank()) return null;
        String key = hash(token);
        Instant now = Instant.now();

        Cached cached = cache.get(key);
        if (cached == null || cached.cachedAt().plus(CACHE_TTL).isBefore(now)) {
            cached = repository.findById(key)
                    .map(s -> new Cached(s.getUserId(), s.getExpiresAt(), s.getLastSeenAt(), now))
                    .orElse(null);
            if (cached == null) {
                cache.remove(key);
                return null;
            }
            cache.put(key, cached);
        }
        if (cached.expiresAt().isBefore(now)) {
            cache.remove(key);
            return null;
        }
        return cached;
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
