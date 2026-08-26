package com.hooppicks.backendapplication.dto;

public record TeamRankDto(
        String id,
        String name,
        String abbreviation,
        String conference,
        String division,
        int rank,
        int eloRating,
        Integer wins,
        Integer losses,
        String streak,
        Integer conferenceSeed,
        String gamesBehind,
        String logoUrl
) {}
