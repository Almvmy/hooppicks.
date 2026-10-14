package com.hooppicks.backendapplication.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Cookie hp_session : même réglage à la connexion et à chaque prolongation de la session. */
@Component
public class SessionCookies {

    private final String sameSite;
    private final boolean secure;

    public SessionCookies(@Value("${app.cookie-same-site:Lax}") String sameSite,
                          @Value("${app.cookie-secure:false}") boolean secure) {
        this.sameSite = sameSite;
        this.secure = secure;
    }

    public ResponseCookie session(String token) {
        return ResponseCookie.from("hp_session", token)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .maxAge(SessionStore.SESSION_TTL)
                .build();
    }
}
