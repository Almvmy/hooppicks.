package com.hooppicks.backendapplication.dto;

/**
 * Tous les champs sont optionnels : ne modifie que ce qui est fourni : sert à
 * corriger manuellement un match dont la synchro externe (balldontlie.io,
 * gratuite) a renvoyé un score ou un statut faux.
 */
public record AdminUpdateMatchRequest(
        String status,
        Integer homeScore,
        Integer awayScore
) {}
