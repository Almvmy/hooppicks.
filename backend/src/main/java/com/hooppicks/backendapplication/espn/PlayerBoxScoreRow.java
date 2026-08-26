package com.hooppicks.backendapplication.espn;

/** Une ligne de feuille de match, déjà parsée depuis la réponse brute d'ESPN. */
public record PlayerBoxScoreRow(
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
        ShotSplit fieldGoals,
        ShotSplit threePoints,
        ShotSplit freeThrows
) {
    public record ShotSplit(int made, int attempted) {}
}
