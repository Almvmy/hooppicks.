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
        String placedAt,
        // Null tant que le ticket est en attente (et sur de vieux tickets).
        String resolvedAt
) {
    public static PlacedBetDto from(Bet bet) {
        return new PlacedBetDto(
                bet.getId(),
                bet.getSelections().stream().map(BetSelectionDto::from).toList(),
                bet.getStake(),
                bet.getTotalOdds(),
                bet.getPotentialPayout(),
                bet.getStatus().name().toLowerCase(),
                bet.getPlacedAt().toString(),
                bet.getResolvedAt() == null ? null : bet.getResolvedAt().toString()
        );
    }
}