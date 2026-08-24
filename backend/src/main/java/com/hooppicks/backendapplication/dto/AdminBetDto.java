package com.hooppicks.backendapplication.dto;

import com.hooppicks.backendapplication.entity.Bet;

import java.util.List;

public record AdminBetDto(
        String id,
        String username,
        List<BetSelectionDto> selections,
        int stake,
        int potentialPayout,
        String placedAt
) {
    public static AdminBetDto from(Bet bet) {
        return new AdminBetDto(
                bet.getId(),
                bet.getUser().getUsername(),
                bet.getSelections().stream().map(BetSelectionDto::from).toList(),
                bet.getStake(),
                bet.getPotentialPayout(),
                bet.getPlacedAt().toString()
        );
    }
}
