package com.hooppicks.backendapplication.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Prolonge la session et son cookie quand le joueur revient (une fois par
 * jour au plus) : sans ça, le cookie expirait 60 jours après la connexion
 * même pour quelqu'un qui joue tous les soirs. En tête de requête, avant que
 * la réponse ne parte. Une panne ici ne bloque jamais la requête.
 */
@Component
public class SessionRenewalFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SessionRenewalFilter.class);

    private final SessionStore sessionStore;
    private final SessionCookies sessionCookies;

    public SessionRenewalFilter(SessionStore sessionStore, SessionCookies sessionCookies) {
        this.sessionStore = sessionStore;
        this.sessionCookies = sessionCookies;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = SessionStore.tokenFrom(request);
        if (token != null && !request.getRequestURI().endsWith("/auth/logout")) {
            try {
                if (sessionStore.renewIfDue(token)) {
                    response.addHeader(HttpHeaders.SET_COOKIE, sessionCookies.session(token).toString());
                }
            } catch (Exception e) {
                log.warn("Prolongation de session impossible : {}", e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }
}
