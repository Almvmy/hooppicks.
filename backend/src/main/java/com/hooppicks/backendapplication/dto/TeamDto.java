package com.hooppicks.backendapplication.dto;

import com.hooppicks.backendapplication.entity.Team;

public record TeamDto(
        String id,
        String name,
        String abbreviation,
        String conference,
        String division,
        String logoUrl
) {
    public static TeamDto from(Team team) {
        return new TeamDto(
                team.getId(),
                team.getName(),
                team.getAbbreviation(),
                team.getConference(),
                team.getDivision(),
                team.getLogoUrl()
        );
    }
}