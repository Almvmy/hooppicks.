package com.hooppicks.backendapplication.dto;

public record LeaderboardEntryDto(
        int rank,
        String username,
        int points,
        int winRate,
        int totalBets
) {}