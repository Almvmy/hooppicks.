package com.hooppicks.backendapplication.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Filet de sécurité pour les exceptions non gérées explicitement dans les
 * contrôleurs : évite de laisser fuir la page d'erreur Spring par défaut
 * (potentiellement une stack trace) jusqu'au client. Les réponses d'erreur déjà
 * écrites à la main dans les contrôleurs (400/401/403 avec message) ne passent
 * pas par ici, seules les exceptions non attrapées y arrivent.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Ressource introuvable."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        e.printStackTrace(); // reste dans les logs serveur, jamais renvoyé au client
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Une erreur est survenue."));
    }
}
