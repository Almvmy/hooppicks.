package com.hooppicks.backendapplication.dto;

public record UpdateNotificationPreferencesRequest(
        boolean notifyMatchStarting,
        boolean notifyBetResults,
        boolean notifyLeagueActivity
) {}
