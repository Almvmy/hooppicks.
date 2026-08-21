package com.hooppicks.backendapplication.dto;

import java.time.Instant;

public record LeagueMemberDto(
        String username,
        boolean isOwner,
        Instant joinedAt
) {}
