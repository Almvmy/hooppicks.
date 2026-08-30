package com.hooppicks.backendapplication.dto;

import jakarta.validation.constraints.NotBlank;

public record ReactRequest(
        @NotBlank String targetType,
        @NotBlank String targetId,
        @NotBlank String emoji
) {}
