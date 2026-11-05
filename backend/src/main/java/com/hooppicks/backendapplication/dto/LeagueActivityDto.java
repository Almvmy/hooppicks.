package com.hooppicks.backendapplication.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record LeagueActivityDto(
        String targetType, // "BET" | "MEMBERSHIP" | "MESSAGE" | "TITLE" : sert de cible pour /leagues/{id}/activity/react
        String targetId,
        String username,
        String message,
        Instant occurredAt,
        int avatarNumber,
        String avatarPosition,
        String avatarColorway,
        String avatarIcon,
        Map<String, Integer> reactionCounts, // emoji -> nombre de réactions
        List<String> myReactions, // emojis avec lesquels l'appelant a déjà réagi
        boolean deletable, // message que l'appelant peut supprimer (le sien, ou s'il dirige la ligue)
        List<CopySelection> selections // ticket d'un autre membre encore jouable : de quoi le reprendre, sinon vide
) {
    public record CopySelection(String matchId, String matchLabel, String market, String outcome, String label,
                                double odds, String playerId, Double line) {}
}
