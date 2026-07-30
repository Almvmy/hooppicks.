package com.hooppicks.backendapplication.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MINUTES = 15;

    private record Attempt(AtomicInteger count, Instant windowStart) {}

    private final ConcurrentHashMap<String, Attempt> attemptsByEmail = new ConcurrentHashMap<>();

    public boolean isBlocked(String email) {
        Attempt attempt = attemptsByEmail.get(email.toLowerCase());
        if (attempt == null) return false;

        boolean windowExpired = Instant.now().isAfter(attempt.windowStart().plusSeconds(WINDOW_MINUTES * 60));
        if (windowExpired) {
            attemptsByEmail.remove(email.toLowerCase());
            return false;
        }
        return attempt.count().get() >= MAX_ATTEMPTS;
    }

    public void recordFailedAttempt(String email) {
        attemptsByEmail.compute(email.toLowerCase(), (key, existing) -> {
            if (existing == null || Instant.now().isAfter(existing.windowStart().plusSeconds(WINDOW_MINUTES * 60))) {
                return new Attempt(new AtomicInteger(1), Instant.now());
            }
            existing.count().incrementAndGet();
            return existing;
        });
    }

    public void recordSuccessfulLogin(String email) {
        attemptsByEmail.remove(email.toLowerCase());
    }
}