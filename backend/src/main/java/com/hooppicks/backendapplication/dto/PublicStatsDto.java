package com.hooppicks.backendapplication.dto;

public record PublicStatsDto(
        long players,
        long bets,
        long leagues,
        long matchesFinished
) {}
