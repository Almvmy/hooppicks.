package com.hooppicks.backendapplication.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionStore {

    // token -> userId. Stocké en mémoire : se vide à chaque redémarrage du serveur (normal pour un apprentissage).
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    public String createSession(String userId) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, userId);
        return token;
    }

    public String getUserId(String token) {
        return sessions.get(token);
    }

    public void invalidate(String token) {
        sessions.remove(token);
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
