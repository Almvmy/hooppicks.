package com.hooppicks.backendapplication.dto;

import java.time.Instant;

public record LeagueActivityDto(
        String username,
        String message,
        Instant occurredAt,
        int avatarNumber,
        String avatarPosition,
        String avatarColorway,
        String avatarIcon
) {}
