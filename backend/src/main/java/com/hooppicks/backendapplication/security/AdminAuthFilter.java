package com.hooppicks.backendapplication.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Protège tout endpoint sous /admin/** avec une clé secrète partagée,
 * transmise dans l'en-tête X-Admin-Key. Volontairement simple (pas de
 * système de rôles) : un seul secret à connaître, à ne jamais exposer côté
 * frontend — seulement toi (curl/Postman) ou un futur job externe l'utilisent.
 *
 * Fail-safe : si admin.api-key n'est pas configurée (ou vide), TOUT accès à
 * /admin/** est refusé plutôt qu'ouvert par défaut.
 */
@Component
public class AdminAuthFilter extends OncePerRequestFilter {

    @Value("${admin.api-key:}")
    private String adminApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/admin/")) {
            String providedKey = request.getHeader("X-Admin-Key");
            if (adminApiKey == null || adminApiKey.isBlank() || !adminApiKey.equals(providedKey)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Clé admin invalide ou manquante.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
