package com.hooppicks.backendapplication.dto;

import com.hooppicks.backendapplication.entity.BetSelection;

public record BetSelectionDto(
        String id,
        String matchId,
        String matchLabel,
        String market,
        String outcome,
        String label,
        double odds
) {
    public static BetSelectionDto from(BetSelection s) {
        return new BetSelectionDto(s.getId(), s.getMatchId(), s.getMatchLabel(), s.getMarket(), s.getOutcome(), s.getLabel(), s.getOdds());
    }
}