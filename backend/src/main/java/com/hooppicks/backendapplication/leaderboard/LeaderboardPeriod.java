package com.hooppicks.backendapplication.leaderboard;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

public enum LeaderboardPeriod {
    SEASON, MONTH, WEEK;

    /** Début de la période (null pour la saison : tous les tickets). */
    public Instant start(ZonedDateTime now) {
        ZonedDateTime midnight = now.toLocalDate().atStartOfDay(now.getZone());
        return switch (this) {
            case SEASON -> null;
            case MONTH -> midnight.withDayOfMonth(1).toInstant();
            case WEEK -> midnight.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toInstant();
        };
    }

    public static LeaderboardPeriod parse(String raw) {
        return switch (raw == null ? "" : raw.toLowerCase()) {
            case "", "season" -> SEASON;
            case "month" -> MONTH;
            case "week" -> WEEK;
            default -> throw new IllegalArgumentException("Période inconnue : " + raw);
        };
    }
}
