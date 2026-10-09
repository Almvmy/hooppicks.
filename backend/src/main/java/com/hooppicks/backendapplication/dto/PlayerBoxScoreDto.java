package com.hooppicks.backendapplication.dto;

import com.hooppicks.backendapplication.entity.PlayerMatchStat;

public record PlayerBoxScoreDto(
        String playerName,
        String teamAbbreviation,
        boolean starter,
        String minutes,
        int points,
        int rebounds,
        int assists,
        int steals,
        int blocks,
        int turnovers,
        int plusMinus,
        String fieldGoals,
        String threePoints,
        String freeThrows
) {
    /** Feuille de match en direct, lue chez ESPN sans passer par la base. */
    public static PlayerBoxScoreDto from(com.hooppicks.backendapplication.espn.PlayerBoxScoreRow r) {
        return new PlayerBoxScoreDto(r.playerName(), r.teamAbbreviation(), r.starter(), r.minutes(), r.points(),
                r.rebounds(), r.assists(), r.steals(), r.blocks(), r.turnovers(), r.plusMinus(),
                r.fieldGoals().made() + "-" + r.fieldGoals().attempted(),
                r.threePoints().made() + "-" + r.threePoints().attempted(),
                r.freeThrows().made() + "-" + r.freeThrows().attempted());
    }

    public static PlayerBoxScoreDto from(PlayerMatchStat s) {
        return new PlayerBoxScoreDto(
                s.getPlayerName(),
                s.getTeamAbbreviation(),
                s.isStarter(),
                s.getMinutes(),
                s.getPoints(),
                s.getRebounds(),
                s.getAssists(),
                s.getSteals(),
                s.getBlocks(),
                s.getTurnovers(),
                s.getPlusMinus(),
                s.getFieldGoalsMade() + "-" + s.getFieldGoalsAttempted(),
                s.getThreePointsMade() + "-" + s.getThreePointsAttempted(),
                s.getFreeThrowsMade() + "-" + s.getFreeThrowsAttempted()
        );
    }
}
