package com.hooppicks.backendapplication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String token,
        @Size(min = 8, message = "Le mot de passe doit faire au moins 8 caractères") String newPassword
) {}
