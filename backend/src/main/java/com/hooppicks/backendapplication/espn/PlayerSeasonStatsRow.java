package com.hooppicks.backendapplication.espn;

/** Moyennes saison d'un joueur (saison la plus récente disponible), déjà parsées depuis ESPN. */
public record PlayerSeasonStatsRow(
        String seasonLabel, // ex. "2025-26"
        int gamesPlayed,
        int gamesStarted,
        double minutesPerGame,
        double pointsPerGame,
        double reboundsPerGame,
        double assistsPerGame,
        double stealsPerGame,
        double blocksPerGame,
        double turnoversPerGame,
        double fieldGoalPct,
        double threePointPct,
        double freeThrowPct
) {
}
