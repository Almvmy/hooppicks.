package com.hooppicks.backendapplication.espn;

/**
 * État en direct d'un match, lu sur le scoreboard ESPN : quart-temps, chrono,
 * score. Affichage seulement : jamais écrit sur le match (le statut et le
 * score en base restent ceux de la synchro, qui règle les paris).
 *
 * @param state  "pre", "in" ou "post"
 * @param detail libellé ESPN brut (« Halftime », « End of 3rd Quarter »…)
 */
public record EspnLiveGame(String eventId, String state, int period, String clock, String detail,
                           Integer homeScore, Integer awayScore) {
}
