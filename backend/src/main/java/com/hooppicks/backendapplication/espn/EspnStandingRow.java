package com.hooppicks.backendapplication.espn;

/** Une ligne de classement, déjà parsée depuis la réponse brute d'ESPN. */
public record EspnStandingRow(
        String teamAbbreviation,
        int wins,
        int losses,
        String streak,
        int conferenceSeed,
        String gamesBehind,
        String logoUrl
) {
}
