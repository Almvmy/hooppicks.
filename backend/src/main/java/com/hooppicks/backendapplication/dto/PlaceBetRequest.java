package com.hooppicks.backendapplication.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PlaceBetRequest(
        @NotEmpty List<@Valid SelectionInput> selections,
        @Min(1) int stake
) {
    public record SelectionInput(
            String matchId,
            String matchLabel,
            String market,
            String outcome,
            String label,
            double odds
    ) {}
}