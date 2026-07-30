package com.hooppicks.backendapplication.dto;


import com.hooppicks.backendapplication.entity.Match;

public record MatchOddsDto(
        double moneylineHome,
        double moneylineAway,
        double spreadValue,
        double spreadOddsHome,
        double spreadOddsAway,
        double totalValue,
        double totalOddsOver,
        double totalOddsUnder
) {
    public static MatchOddsDto from(Match match) {
        return new MatchOddsDto(
                match.getMoneylineHome(),
                match.getMoneylineAway(),
                match.getSpreadValue(),
                match.getSpreadOddsHome(),
                match.getSpreadOddsAway(),
                match.getTotalValue(),
                match.getTotalOddsOver(),
                match.getTotalOddsUnder()
        );
    }
}