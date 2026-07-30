package com.hooppicks.backendapplication.nba.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NbaGameDto(
        Long id,
        String date,
        String status,
        String datetime,
        @JsonProperty("home_team_score") Integer homeTeamScore,
        @JsonProperty("visitor_team_score") Integer visitorTeamScore,
        @JsonProperty("home_team") NbaTeamDto homeTeam,
        @JsonProperty("visitor_team") NbaTeamDto visitorTeam
) {}