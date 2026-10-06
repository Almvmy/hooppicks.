package com.hooppicks.backendapplication.dto;

public record UpdateNotificationPreferencesRequest(
        boolean notifyMatchStarting,
        boolean notifyBetResults,
        boolean notifyLeagueActivity,
        // Nullable : un client qui ne connaît pas encore ce réglage ne
        // l'écrase pas en l'omettant.
        Boolean notifyFavoriteTeam
) {}
