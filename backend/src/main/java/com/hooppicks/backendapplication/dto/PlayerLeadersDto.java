package com.hooppicks.backendapplication.dto;

import java.util.List;

/** Top 5 par catégorie statistique : affiché par défaut sur la page joueurs, avant toute recherche. */
public record PlayerLeadersDto(
        List<RosterPlayerDto> points,
        List<RosterPlayerDto> rebounds,
        List<RosterPlayerDto> assists
) {
}
