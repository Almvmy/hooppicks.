package com.hooppicks.backendapplication.dto;

public record BadgeDto(
        String id,
        String label,
        String description,
        boolean unlocked
) {
}