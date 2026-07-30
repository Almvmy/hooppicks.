package com.hooppicks.backendapplication.dto;

import com.hooppicks.backendapplication.entity.Bet;

import java.util.List;

public record PlacedBetDto(
        String id,
        List<BetSelectionDto> selections,
        int stake,
        double totalOdds,
        int potentialPayout,
        String status,
        String placedAt
) {
    public static PlacedBetDto from(Bet bet) {
        return new PlacedBetDto(
                bet.getId(),
                bet.getSelections().stream().map(BetSelectionDto::from).toList(),
                bet.getStake(),
                bet.getTotalOdds(),
                bet.getPotentialPayout(),
                bet.getStatus().name().toLowerCase(),
                bet.getPlacedAt().toString()
        );
    }
}