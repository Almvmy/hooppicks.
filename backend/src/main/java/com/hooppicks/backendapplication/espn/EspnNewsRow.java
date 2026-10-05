package com.hooppicks.backendapplication.espn;

import java.time.Instant;
import java.util.List;

/**
 * Un article de l'API d'actualités ESPN, déjà parsé. teamAbbreviations est
 * aux sigles balldontlie (ceux de notre base), pas aux sigles ESPN.
 */
public record EspnNewsRow(
        String headline,
        String description,
        String link,
        Instant publishedAt,
        String imageUrl,
        List<String> teamAbbreviations,
        boolean video
) {
}
