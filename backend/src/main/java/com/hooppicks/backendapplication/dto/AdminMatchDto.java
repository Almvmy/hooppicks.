package com.hooppicks.backendapplication.dto;

/**
 * Match vu depuis la console admin : verrouillé ou non (corrigé à la main),
 * et combien de paris en dépendent, pour mesurer l'effet d'une correction
 * avant de la faire.
 */
public record AdminMatchDto(
        MatchDto match,
        boolean locked,
        long pendingBets,
        long resolvedBets
) {}
