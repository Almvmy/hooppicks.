package com.hooppicks.backendapplication.dto;

/** Une des meilleures lignes de stats de la dernière soirée (page Joueurs). player : null si absent des effectifs. */
public record TopPerformanceDto(
        String playerName,
        String teamAbbreviation,
        String opponentAbbreviation,
        String matchId,
        RosterPlayerDto player,
        int minutes,
        int points,
        int rebounds,
        int assists,
        int steals,
        int blocks,
        int threePointsMade,
        int fieldGoalsMade,
        int fieldGoalsAttempted
) {
}
