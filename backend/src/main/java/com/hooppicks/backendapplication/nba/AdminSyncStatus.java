package com.hooppicks.backendapplication.nba;

import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * État en mémoire du dernier passage de synchro NBA, pour l'interface admin.
 * Pas de persistance DB : se vide au redémarrage, comme SessionStore, c'est
 * juste un instantané de ce que le process a fait récemment.
 */
@Component
public class AdminSyncStatus {

    private volatile Instant lastSyncAt;
    private volatile int lastGamesSynced;
    private volatile int lastBetsResolved;
    private volatile String mode;

    public void recordSync(int gamesSynced, int betsResolved, String mode) {
        this.lastSyncAt = Instant.now();
        this.lastGamesSynced = gamesSynced;
        this.lastBetsResolved = betsResolved;
        this.mode = mode;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public int getLastGamesSynced() {
        return lastGamesSynced;
    }

    public int getLastBetsResolved() {
        return lastBetsResolved;
    }

    public String getMode() {
        return mode;
    }
}
