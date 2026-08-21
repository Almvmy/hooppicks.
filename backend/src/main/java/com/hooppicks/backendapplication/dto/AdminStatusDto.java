package com.hooppicks.backendapplication.dto;

import java.time.Instant;

public record AdminStatusDto(
        Instant lastSyncAt,
        int lastGamesSynced,
        int lastBetsResolved,
        String syncMode,
        long totalUsers,
        long totalMatches,
        long pendingBets
) {}
