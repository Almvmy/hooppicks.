package com.hooppicks.backendapplication.dto;

import java.util.List;

/** Classement plus long d'une catégorie de meneurs (même saison et même minimum de matchs que PlayerLeadersDto). */
public record LeaderListDto(List<RosterPlayerDto> players, String season, int minGames) {
}
