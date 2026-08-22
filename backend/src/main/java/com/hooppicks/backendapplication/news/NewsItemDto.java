package com.hooppicks.backendapplication.news;

import java.time.Instant;

public record NewsItemDto(String title, String link, String description, String source, Instant publishedAt) {
}
