package com.hooppicks.backendapplication.leaderboard;

import com.hooppicks.backendapplication.bankroll.BankrollService;

import java.time.Instant;
import java.time.ZonedDateTime;

public enum LeaderboardPeriod {
    SEASON, MONTH, WEEK;

    /** Début de la période (null pour la saison : tous les tickets). */
    public Instant start(ZonedDateTime now) {
        ZonedDateTime midnight = now.toLocalDate().atStartOfDay(now.getZone());
        return switch (this) {
            case SEASON -> null;
            case MONTH -> midnight.withDayOfMonth(1).toInstant();
            // La semaine du classement est la semaine de jeu (lundi 12h), pas
            // la semaine civile : celle où tout le monde est reparti à égalité.
            case WEEK -> BankrollService.weekStart(now.toInstant()).toInstant();
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
