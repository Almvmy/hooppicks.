package com.hooppicks.backendapplication.espn;

import com.hooppicks.backendapplication.entity.MatchStatus;

import java.time.Instant;

/**
 * Un match du scoreboard ESPN, sigles déjà convertis aux nôtres
 * (balldontlie). seasonType : 1 présaison, 2 saison régulière, 3 playoffs,
 * 5 play-in. competitionType : "ALLSTAR" pour l'All-Star Game, "STD" sinon
 * (ou le tour des playoffs). note : intitulé ESPN ("NBA Cup - Quarterfinals",
 * "East 1st Round - Game 2"…).
 */
public record EspnGameRow(
        String eventId,
        Instant date,
        String homeAbbreviation,
        String awayAbbreviation,
        Integer homeScore,
        Integer awayScore,
        String state,
        boolean completed,
        int seasonType,
        String competitionType,
        String note,
        String seriesSummary
) {
    public MatchStatus matchStatus() {
        if ("in".equals(state)) return MatchStatus.LIVE;
        // "post" sans "completed" = reporté ou annulé : on le laisse à venir
        // plutôt que de le clore avec un score vide.
        if ("post".equals(state) && completed) return MatchStatus.FINISHED;
        return MatchStatus.SCHEDULED;
    }
}
