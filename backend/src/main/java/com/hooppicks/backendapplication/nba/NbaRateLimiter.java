package com.hooppicks.backendapplication.nba;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Garde-fou pour ne jamais dépasser le quota gratuit balldontlie
 * (5 req/min), même si plusieurs recherches "cache miss" arrivent en même
 * temps côté utilisateurs. Fenêtre glissante de 60s ; au-delà de la limite,
 * on refuse l'appel externe plutôt que de faire attendre l'utilisateur —
 * l'appelant retombe alors sur le cache local (potentiellement incomplet).
 */
@Component
public class NbaRateLimiter {

    private static final int MAX_CALLS_PER_WINDOW = 4; // marge de sécurité sous les 5/min officielles
    private static final long WINDOW_MS = 60_000;

    private final Deque<Long> callTimestamps = new ArrayDeque<>();

    public synchronized boolean tryAcquire() {
        long now = Instant.now().toEpochMilli();
        while (!callTimestamps.isEmpty() && now - callTimestamps.peekFirst() > WINDOW_MS) {
            callTimestamps.pollFirst();
        }
        if (callTimestamps.size() >= MAX_CALLS_PER_WINDOW) {
            return false;
        }
        callTimestamps.addLast(now);
        return true;
    }
}
