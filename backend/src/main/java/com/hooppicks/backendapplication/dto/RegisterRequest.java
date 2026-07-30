package com.hooppicks.backendapplication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String username,
        @Email @NotBlank String email,
        @Size(min = 6, message = "Le mot de passe doit faire au moins 6 caractères") String password
) {}