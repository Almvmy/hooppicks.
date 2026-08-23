package com.hooppicks.backendapplication.dto;

public record LeaderboardEntryDto(
        int rank,
        String username,
        int points,
        int winRate,
        int totalBets,
        int avatarNumber,
        String avatarPosition,
        String avatarColorway,
        String avatarIcon
) {}
