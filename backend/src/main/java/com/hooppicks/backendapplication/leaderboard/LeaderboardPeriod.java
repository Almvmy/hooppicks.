package com.hooppicks.backendapplication.leaderboard;

import com.hooppicks.backendapplication.bankroll.BankrollService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Semaine et mois : tous les tickets de la période. Saison, Coupe NBA et
 * playoffs : les tickets de la phase (`BetRepository.PHASE_FILTER`) depuis
 * le début de la saison NBA en cours.
 */
public enum LeaderboardPeriod {
    SEASON, MONTH, WEEK, CUP, PLAYOFFS;

    public Instant start(ZonedDateTime now) {
        ZonedDateTime midnight = now.toLocalDate().atStartOfDay(now.getZone());
        return switch (this) {
            case SEASON, CUP, PLAYOFFS -> seasonStart(now);
            case MONTH -> midnight.withDayOfMonth(1).toInstant();
            // La semaine du classement est la semaine de jeu (lundi 12h), pas
            // la semaine civile : celle où tout le monde est reparti à égalité.
            case WEEK -> BankrollService.weekStart(now.toInstant()).toInstant();
        };
    }

    /** Valeur de `:phase` dans les requêtes du classement. */
    public String phase() {
        return switch (this) {
            case SEASON -> "SEASON";
            case CUP -> "CUP";
            case PLAYOFFS -> "PLAYOFFS";
            case MONTH, WEEK -> "ALL";
        };
    }

    /** 1er septembre de la saison en cours (même bascule que `SeasonCalendar.currentSeason`). */
    public static Instant seasonStart(ZonedDateTime now) {
        ZonedDateTime t = now.withZoneSameInstant(BankrollService.ZONE);
        int year = t.getMonthValue() >= 9 ? t.getYear() : t.getYear() - 1;
        return LocalDate.of(year, 9, 1).atStartOfDay(BankrollService.ZONE).toInstant();
    }

    public static LeaderboardPeriod parse(String raw) {
        return switch (raw == null ? "" : raw.toLowerCase()) {
            case "", "season" -> SEASON;
            case "month" -> MONTH;
            case "week" -> WEEK;
            case "cup" -> CUP;
            case "playoffs" -> PLAYOFFS;
            default -> throw new IllegalArgumentException("Période inconnue : " + raw);
        };
    }
}
