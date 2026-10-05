package com.hooppicks.backendapplication.news;

import java.time.Instant;
import java.util.List;

/**
 * imageUrl, teams (sigles de notre base) et video ne sont renseignés que par
 * l'API JSON d'ESPN ; en repli sur le flux RSS : null, liste vide, false.
 */
public record NewsItemDto(
        String title,
        String link,
        String description,
        String source,
        Instant publishedAt,
        String imageUrl,
        List<String> teams,
        boolean video
) {
    public static NewsItemDto fromRss(String title, String link, String description, Instant publishedAt) {
        return new NewsItemDto(title, link, description, "ESPN", publishedAt, null, List.of(), false);
    }

    public NewsItemDto withText(String newTitle, String newDescription) {
        return new NewsItemDto(newTitle, link, newDescription, source, publishedAt, imageUrl, teams, video);
    }
}
