package com.hooppicks.backendapplication.dto;

/**
 * Répartition des paris de la communauté sur ce match, par marché : champ
 * null si personne n'a encore parié sur ce marché (distinct de 0%, qui
 * voudrait dire "au moins un pari, mais jamais sur ce côté").
 */
public record PickPercentagesDto(
        Integer moneylineHomePct,
        Integer moneylineAwayPct,
        Integer spreadHomePct,
        Integer spreadAwayPct,
        Integer totalOverPct,
        Integer totalUnderPct
) {
}
