package com.hooppicks.backendapplication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String username,
        @Email @NotBlank String email,
        @Size(min = 8, message = "Le mot de passe doit faire au moins 8 caractères") String password,
        /** Pseudo du parrain (lien d'invitation), facultatif. */
        String ref
) {}