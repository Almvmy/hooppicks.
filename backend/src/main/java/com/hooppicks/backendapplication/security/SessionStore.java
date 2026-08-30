package com.hooppicks.backendapplication.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionStore {

    // Aligné sur le maxAge du cookie hp_session posé par AuthController.setSessionCookie
    private static final long SESSION_TTL_SECONDS = 60 * 60 * 24;

    private record SessionData(String userId, Instant createdAt) {}

    // token -> session. Stocké en mémoire : se vide à chaque redémarrage du serveur (normal pour un apprentissage).
    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public String createSession(String userId) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new SessionData(userId, Instant.now()));
        return token;
    }

    public String getUserId(String token) {
        SessionData data = sessions.get(token);
        if (data == null) return null;

        if (Instant.now().isAfter(data.createdAt().plusSeconds(SESSION_TTL_SECONDS))) {
            sessions.remove(token);
            return null;
        }
        return data.userId();
    }

    public void invalidate(String token) {
        sessions.remove(token);
    }

    /**
     * Invalide toutes les sessions actives d'un utilisateur : utilisé après un
     * changement de mot de passe (reset), pour ne pas laisser valide une session
     * déjà ouverte par quelqu'un d'autre si le compte a été compromis.
     */
    public void invalidateAllForUser(String userId) {
        sessions.entrySet().removeIf(entry -> entry.getValue().userId().equals(userId));
    }

    public String getUserIdFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals("hp_session")) {
                return getUserId(cookie.getValue());
            }
        }
        return null;
    }
}
