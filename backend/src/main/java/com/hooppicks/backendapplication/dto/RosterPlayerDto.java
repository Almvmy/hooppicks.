package com.hooppicks.backendapplication.dto;

import com.hooppicks.backendapplication.entity.Player;
import com.hooppicks.backendapplication.entity.RosterPlayer;

public record RosterPlayerDto(
        String id,
        String firstName,
        String lastName,
        String position,
        String jersey,
        String height,
        String weight,
        String headshotUrl,
        TeamDto team,
        String injuryStatus,
        String statsSeasonLabel,
        Integer gamesPlayed,
        Integer gamesStarted,
        Double minutesPerGame,
        Double pointsPerGame,
        Double reboundsPerGame,
        Double assistsPerGame,
        Double stealsPerGame,
        Double blocksPerGame,
        Double turnoversPerGame,
        Double fieldGoalPct,
        Double threePointPct,
        Double freeThrowPct
) {
    public static RosterPlayerDto from(RosterPlayer p) {
        return new RosterPlayerDto(
                p.getId(),
                p.getFirstName(),
                p.getLastName(),
                p.getPosition(),
                p.getJersey(),
                p.getHeight(),
                p.getWeight(),
                p.getHeadshotUrl(),
                p.getTeam() != null ? TeamDto.from(p.getTeam()) : null,
                p.getInjuryStatus(),
                p.getStatsSeasonLabel(),
                p.getGamesPlayed(),
                p.getGamesStarted(),
                p.getMinutesPerGame(),
                p.getPointsPerGame(),
                p.getReboundsPerGame(),
                p.getAssistsPerGame(),
                p.getStealsPerGame(),
                p.getBlocksPerGame(),
                p.getTurnoversPerGame(),
                p.getFieldGoalPct(),
                p.getThreePointPct(),
                p.getFreeThrowPct()
        );
    }

    /**
     * Résultat de secours balldontlie (cf. NbaSyncService.searchAndCachePlayers) —
     * même forme que la réponse normale pour que le frontend n'ait rien de
     * spécial à gérer, juste beaucoup plus de champs à null (pas de photo,
     * pas de blessure, pas de stats saison : balldontlie free tier ne les a pas).
     */
    public static RosterPlayerDto fromBalldontlie(Player p) {
        return new RosterPlayerDto(
                p.getId(),
                p.getFirstName(),
                p.getLastName(),
                p.getPosition(),
                null,
                p.getHeight(),
                p.getWeight(),
                null,
                p.getTeam() != null ? TeamDto.from(p.getTeam()) : null,
                null,
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );
    }
}
