package com.hooppicks.backendapplication.dto;

import com.hooppicks.backendapplication.espn.PlayerRecentGameRow;

public record PlayerRecentGameDto(
        String date,
        String opponentAbbreviation,
        String result,
        String score,
        String minutes,
        int points,
        int rebounds,
        int assists
) {
    public static PlayerRecentGameDto from(PlayerRecentGameRow row) {
        return new PlayerRecentGameDto(
                row.date(),
                row.opponentAbbreviation(),
                row.result(),
                row.score(),
                row.minutes(),
                row.points(),
                row.rebounds(),
                row.assists()
        );
    }
}
