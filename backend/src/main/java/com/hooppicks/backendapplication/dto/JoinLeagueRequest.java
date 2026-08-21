package com.hooppicks.backendapplication.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinLeagueRequest(@NotBlank String inviteCode) {}
