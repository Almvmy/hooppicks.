package com.hooppicks.backendapplication.dto;

import java.time.Instant;

public record LeagueDto(
        String id,
        String name,
        String inviteCode,
        long memberCount,
        boolean isOwner,
        Instant createdAt
) {}
