package com.hooppicks.backendapplication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    // Dev seulement : tester sur un vrai téléphone via l'IP du PC sur le Wi-Fi
    // (http://10.x.x.x:3000). Le relais /api de Next transmet l'origine du
    // navigateur, refusée sinon. Vide par défaut : en production, seule
    // app.frontend-url passe, et c'est ce contrôle qui empêche un autre site
    // de faire agir un joueur connecté à son insu.
    @Value("${app.cors.dev-origin-patterns:}")
    private String[] devOriginPatterns;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                var mapping = registry.addMapping("/**").allowedOrigins(frontendUrl);
                if (devOriginPatterns.length > 0 && !devOriginPatterns[0].isBlank()) {
                    mapping.allowedOriginPatterns(devOriginPatterns);
                }
                mapping
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}