package com.hooppicks.backendapplication.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateLeagueRequest(@NotBlank String name) {}
