package com.hooppicks.backendapplication.espn;

/** Une ligne de "forme récente" (derniers matchs joués), déjà parsée depuis ESPN. */
public record PlayerRecentGameRow(
        String date, // ISO-8601, ex. "2026-03-12T01:00:00.000+00:00"
        String opponentAbbreviation,
        String result, // "W" ou "L"
        String score,
        String minutes,
        int points,
        int rebounds,
        int assists
) {
}
