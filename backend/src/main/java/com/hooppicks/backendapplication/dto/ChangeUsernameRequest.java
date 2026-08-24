package com.hooppicks.backendapplication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeUsernameRequest(
        @NotBlank @Size(min = 3, max = 20, message = "Le pseudo doit faire entre 3 et 20 caractères") String newUsername,
        @NotBlank String currentPassword
) {}
