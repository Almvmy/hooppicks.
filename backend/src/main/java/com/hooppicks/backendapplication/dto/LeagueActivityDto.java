package com.hooppicks.backendapplication.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record LeagueActivityDto(
        String targetType, // "BET" | "MEMBERSHIP" : sert de cible pour /leagues/{id}/activity/react
        String targetId,
        String username,
        String message,
        Instant occurredAt,
        int avatarNumber,
        String avatarPosition,
        String avatarColorway,
        String avatarIcon,
        Map<String, Integer> reactionCounts, // emoji -> nombre de réactions
        List<String> myReactions // emojis avec lesquels l'appelant a déjà réagi
) {}
