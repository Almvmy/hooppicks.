package com.hooppicks.backendapplication.bankroll;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * Le jour du quiz et du débat : 8h GMT → 8h GMT le lendemain (choix du propriétaire).
 * À 12h comme la soirée NBA, celui qui jouait au réveil tombait sur le quiz de la
 * veille, qui parlait d'une nuit vieille de plus d'un jour, et le débat sur l'exploit
 * de la nuit n'arrivait qu'à midi. 8h et pas plus tôt : les derniers matchs de la
 * côte Ouest finissent vers 6h GMT (plus tard avec une prolongation), et leurs
 * feuilles de match arrivent dans les 20 minutes.
 *
 * Le fil de la soirée, la cote boostée et la semaine de jeu restent à 12h : le matin,
 * on réagit encore aux résultats dans le fil de la nuit.
 */
public final class GameDay {

    public static final LocalTime START = LocalTime.of(8, 0);

    private GameDay() {}

    public static LocalDate of(Instant at) {
        ZonedDateTime t = at.atZone(BankrollService.ZONE);
        return t.toLocalTime().isBefore(START) ? t.toLocalDate().minusDays(1) : t.toLocalDate();
    }

    /** Fin de « la nuit dernière » pour ce jour : les matchs finis avant son ouverture. */
    public static Instant start(LocalDate day) {
        return day.atTime(START).atZone(BankrollService.ZONE).toInstant();
    }
}
