package com.hooppicks.backendapplication.espn;

/** Une ligne d'effectif d'équipe, déjà parsée depuis la réponse brute d'ESPN. */
public record EspnRosterRow(
        String espnId,
        String firstName,
        String lastName,
        String position,
        String jersey,
        String height,
        String weight,
        String headshotUrl,
        String injuryStatus, // ex. "Day-To-Day", "Out" (null si pas blessé)
        String injuryDate // ISO-8601 brut ESPN, ex. "2026-07-27T16:11Z"
) {
}
