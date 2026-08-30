package com.hooppicks.backendapplication.dto;

import com.hooppicks.backendapplication.entity.Match;

public record MatchDto(
        String id,
        TeamDto homeTeam,
        TeamDto awayTeam,
        String date,
        String status,
        Integer homeScore,
        Integer awayScore,
        MatchOddsDto odds,
        PickPercentagesDto pickPercentages
) {
    public static MatchDto from(Match match) {
        return from(match, null);
    }

    public static MatchDto from(Match match, PickPercentagesDto pickPercentages) {
        return new MatchDto(
                match.getId(),
                TeamDto.from(match.getHomeTeam()),
                TeamDto.from(match.getAwayTeam()),
                match.getDate().toString(),
                match.getStatus().name().toLowerCase(), // "SCHEDULED" -> "scheduled", pour matcher ton type MatchStatus TS
                match.getHomeScore(),
                match.getAwayScore(),
                MatchOddsDto.from(match),
                pickPercentages
        );
    }
}
