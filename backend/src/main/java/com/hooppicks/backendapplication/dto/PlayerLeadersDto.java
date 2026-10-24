package com.hooppicks.backendapplication.dto;

import java.util.List;

/**
 * Top 5 par catégorie statistique (cf. StatLeadersService) : affiché par
 * défaut sur la page joueurs. season : saison des moyennes, minGames : matchs
 * joués exigés pour y figurer.
 */
public record PlayerLeadersDto(
        List<RosterPlayerDto> points,
        List<RosterPlayerDto> rebounds,
        List<RosterPlayerDto> assists,
        List<RosterPlayerDto> steals,
        List<RosterPlayerDto> blocks,
        List<RosterPlayerDto> threes,
        List<RosterPlayerDto> fieldGoalPct,
        String season,
        int minGames
) {
}
