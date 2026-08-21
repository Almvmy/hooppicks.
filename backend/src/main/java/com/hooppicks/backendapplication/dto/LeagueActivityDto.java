package com.hooppicks.backendapplication.dto;

import java.time.Instant;

public record LeagueActivityDto(
        String username,
        String message,
        Instant occurredAt
) {}
