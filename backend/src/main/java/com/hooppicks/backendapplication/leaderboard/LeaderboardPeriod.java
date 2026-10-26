package com.hooppicks.backendapplication.leaderboard;

import com.hooppicks.backendapplication.bankroll.BankrollService;
import com.hooppicks.backendapplication.palmares.PalmaresService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Semaine et mois : tous les tickets de la période. Saison, Coupe NBA,
 * playoffs et général (toute la saison, toutes compétitions) : les tickets
 * de la phase (`BetRepository.PHASE_FILTER`) depuis le début de la saison
 * NBA en cours.
 */
public enum LeaderboardPeriod {
    SEASON, MONTH, WEEK, CUP, PLAYOFFS, GENERAL;

    public Instant start(ZonedDateTime now) {
        ZonedDateTime midnight = now.toLocalDate().atStartOfDay(now.getZone());
        return switch (this) {
            case SEASON, CUP, PLAYOFFS, GENERAL -> seasonStart(now);
            case MONTH -> monthStart(midnight.withDayOfMonth(1).toLocalDate());
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
            case GENERAL -> "GENERAL";
            case MONTH, WEEK -> "ALL";
        };
    }

    /**
     * Début du classement d'un mois : ses semaines de jeu (lundi dans le
     * mois), sans les semaines d'essai d'avant le lancement : le champion du
     * mois d'octobre 2026 est le n° 1 de ce classement, pas d'un autre.
     */
    public static Instant monthStart(LocalDate firstDay) {
        return firstDay.isBefore(PalmaresService.FIRST_WEEK)
                ? BankrollService.startOf(PalmaresService.FIRST_WEEK)
                : firstDay.atStartOfDay(BankrollService.ZONE).toInstant();
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
            case "general" -> GENERAL;
            default -> throw new IllegalArgumentException("Période inconnue : " + raw);
        };
    }
}
