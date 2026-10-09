package com.hooppicks.backendapplication.espn;

import java.time.Instant;

/**
 * Un match terminé, réduit au strict nécessaire (le calendrier ESPN pèse
 * ~19 Ko par match à cause des meilleurs joueurs embarqués). Sigles
 * balldontlie (les nôtres).
 */
public record EspnPastGame(Instant date, String homeAbbreviation, String awayAbbreviation, int homeScore,
                           int awayScore, boolean playoffs) {
}
